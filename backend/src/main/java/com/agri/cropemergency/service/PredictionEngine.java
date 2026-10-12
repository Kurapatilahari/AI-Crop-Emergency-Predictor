package com.agri.cropemergency.service;

import com.agri.cropemergency.model.FarmField;
import com.agri.cropemergency.model.RiskLevel;
import com.agri.cropemergency.model.SensorReading;
import com.agri.cropemergency.model.Threat;
import com.agri.cropemergency.service.CropProfiles.CropProfile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Core prediction engine.
 * <p>
 * 1. Derives stress indicators from fused weather + soil + NDVI data
 *    (moisture deficit, heat-stress index, NDVI decline rate via time-series regression).
 * 2. Scores six threat types 0-100 and applies learned per-threat sensitivity (feedback loop).
 * 3. Converts the highest score into a Green / Amber / Red level and estimates WHEN it will hit.
 * <p>
 * The scoring is a transparent, explainable model that runs without any external ML runtime.
 * It is deliberately isolated behind {@link #evaluate} so an LSTM / XGBoost service can replace it.
 */
@Component
public class PredictionEngine {

    public record Result(Threat threat, RiskLevel risk, double score, String severity, int daysToImpact,
                         double moistureDeficit, double heatStressIndex, double ndviDeclineRate,
                         Map<String, Double> scores) {
    }

    public Result evaluate(FarmField field, List<SensorReading> readingsAsc, WeatherForecast wf,
                           Map<String, Double> sensitivity) {

        CropProfile p = CropProfiles.forCrop(field.getCropType());
        SensorReading last = readingsAsc.get(readingsAsc.size() - 1);

        double moisture = nz(last.getSoilMoisture(), (p.minMoisture() + p.maxMoisture()) / 2);
        double temp = nz(last.getTemperature(), 28);
        double hum = nz(last.getHumidity(), 60);
        double lastRain = nz(last.getRainfall(), 0);

        double rain3d = wf != null ? wf.rain3d() : 0;
        double fMaxTemp = wf != null ? wf.maxTemp() : temp;

        // ---- time-series trend features ----
        double moistureSlope = slopePerDay(readingsAsc, SensorReading::getSoilMoisture);
        double ndviSlope = slopePerDay(readingsAsc, SensorReading::getNdvi);
        double ndviDecline = Math.max(0, -ndviSlope);
        double ndviFactor = clamp01(ndviDecline / 0.02);
        double moistureDecline = clamp01(Math.max(0, -moistureSlope) / 2.0);

        // ---- DROUGHT ----
        double deficit = Math.max(0, (p.minMoisture() - moisture) / p.minMoisture());
        double projMoisture = moisture + moistureSlope * 3;
        double projDeficit = Math.max(0, (p.minMoisture() - projMoisture) / p.minMoisture());
        double effDeficit = clamp01(Math.max(deficit, projDeficit * 0.9) / 0.5);
        double droughtCause = 0.75 * effDeficit + 0.25 * moistureDecline;
        if (rain3d >= 20) droughtCause *= 0.6;
        else if (rain3d >= 8) droughtCause *= 0.8;
        double drought = 100 * droughtCause * (0.7 + 0.3 * ndviFactor);

        // ---- HEAT STRESS ----
        double effTemp = Math.max(temp, fMaxTemp);
        double hsi = effTemp <= p.heatThreshold() ? 0
                : clamp01((effTemp - p.heatThreshold()) * (1 + hum / 200.0) / 8.0);
        double heat = 100 * hsi * (0.75 + 0.25 * ndviFactor);

        // ---- WATERLOGGING ----
        double range = Math.max(1, 100 - p.maxMoisture());
        double excess = Math.max(0, (moisture - p.maxMoisture()) / range);
        double projExcess = Math.max(0, (moisture + moistureSlope * 3 - p.maxMoisture()) / range);
        double effExcess = clamp01(Math.max(excess, projExcess * 0.9) / 0.5);
        double rainFactor = clamp01(Math.max(rain3d / 60.0, lastRain / 40.0));
        double water = 100 * (0.7 * effExcess + 0.3 * rainFactor) * (0.7 + 0.3 * ndviFactor);

        // ---- NUTRIENT DEFICIENCY ----
        double shortSum = 0;
        int shortN = 0;
        if (last.getNitrogen() != null)   { shortSum += clamp01((p.minN() - last.getNitrogen()) / p.minN()); shortN++; }
        if (last.getPhosphorus() != null) { shortSum += clamp01((p.minP() - last.getPhosphorus()) / p.minP()); shortN++; }
        if (last.getPotassium() != null)  { shortSum += clamp01((p.minK() - last.getPotassium()) / p.minK()); shortN++; }
        double nutrientCause = shortN > 0 ? clamp01((shortSum / shortN) / 0.6) : 0;
        double nutrient = 100 * nutrientCause * (0.6 + 0.4 * ndviFactor);

        // ---- PEST OUTBREAK (warm + humid favours pests) ----
        double tempFit = 1 - clamp01(Math.abs(temp - 29) / 6.0);
        double humFit = clamp01((hum - 60) / 30.0);
        double pest = 100 * (tempFit * humFit) * (0.45 + 0.55 * ndviFactor);

        // ---- SALINITY / pH ----
        double salFactor = last.getSalinity() == null ? 0 : clamp01((last.getSalinity() - 2) / 4.0);
        double phFactor = 0;
        if (last.getSoilPh() != null) {
            double ph = last.getSoilPh();
            if (ph < p.phMin()) phFactor = clamp01((p.phMin() - ph) / 1.5);
            else if (ph > p.phMax()) phFactor = clamp01((ph - p.phMax()) / 1.5);
        }
        double saline = 100 * (0.65 * salFactor + 0.35 * phFactor) * (0.75 + 0.25 * ndviFactor);

        // ---- apply learned sensitivity & pick the dominant threat ----
        EnumMap<Threat, Double> raw = new EnumMap<>(Threat.class);
        raw.put(Threat.DROUGHT, drought);
        raw.put(Threat.HEAT_STRESS, heat);
        raw.put(Threat.WATERLOGGING, water);
        raw.put(Threat.NUTRIENT_DEFICIENCY, nutrient);
        raw.put(Threat.PEST_OUTBREAK, pest);
        raw.put(Threat.SALINITY_STRESS, saline);

        Map<String, Double> scores = new LinkedHashMap<>();
        Threat top = Threat.NONE;
        double topScore = -1;
        for (Map.Entry<Threat, Double> e : raw.entrySet()) {
            double s = e.getValue() * sensitivity.getOrDefault(e.getKey().name(), 1.0);
            s = round1(Math.max(0, Math.min(100, s)));
            scores.put(e.getKey().name(), s);
            if (s > topScore) {
                topScore = s;
                top = e.getKey();
            }
        }

        RiskLevel risk = topScore >= 65 ? RiskLevel.RED : topScore >= 35 ? RiskLevel.AMBER : RiskLevel.GREEN;
        String severity = topScore >= 85 ? "CRITICAL" : topScore >= 65 ? "SEVERE" : topScore >= 35 ? "MODERATE" : "LOW";
        if (risk == RiskLevel.GREEN) top = Threat.NONE;

        int days = estimateDays(top, risk, topScore, moisture, moistureSlope, p);

        return new Result(top, risk, topScore, severity, days,
                round3(deficit), round3(hsi), round3(ndviDecline), scores);
    }

    /** When is the threat likely to occur? */
    private int estimateDays(Threat t, RiskLevel risk, double score, double moisture, double slope, CropProfile p) {
        if (risk == RiskLevel.GREEN) return 0;
        if (t == Threat.DROUGHT && slope < -0.05) {
            double critical = p.minMoisture() * 0.7;
            if (moisture <= critical) return 1;
            int d = (int) Math.round((moisture - critical) / -slope);
            return Math.max(1, Math.min(14, d));
        }
        if (risk == RiskLevel.RED) return score >= 85 ? 1 : 2;
        return 3 + (int) Math.round((65 - score) / 30.0 * 4);   // amber: 3..7 days
    }

    public static String recommend(Threat t, RiskLevel risk, int days) {
        if (risk == RiskLevel.GREEN || t == Threat.NONE) {
            return "Crop conditions look healthy. Continue regular monitoring and keep sensor data up to date.";
        }
        boolean red = risk == RiskLevel.RED;
        return switch (t) {
            case DROUGHT -> red
                    ? "Irrigate immediately (early morning or evening). Apply mulch to cut evaporation and postpone fertilizer until soil moisture recovers."
                    : "Plan irrigation within " + days + " day(s). Check soil moisture daily and consider mulching to hold moisture.";
            case HEAT_STRESS -> red
                    ? "Irrigate lightly in the evening to cool the canopy, use shade nets where possible, consider a foliar potassium spray, and avoid fertilizing or spraying in midday heat."
                    : "Heat is building. Prepare light irrigation or shade cover and watch the canopy over the next " + days + " day(s).";
            case WATERLOGGING -> red
                    ? "Open drainage channels now, stop irrigation, avoid nitrogen application and pump out standing water to protect roots."
                    : "Soil is getting too wet. Clear drains and pause irrigation; heavy rain could cause waterlogging in about " + days + " day(s).";
            case NUTRIENT_DEFICIENCY -> red
                    ? "Apply a balanced NPK dose or targeted foliar spray based on a soil test, split over two weeks. Check leaves for yellowing or purpling."
                    : "Nutrient levels are dropping. Plan a top-dressing for the lacking nutrient within " + days + " day(s) and confirm with a soil test.";
            case PEST_OUTBREAK -> red
                    ? "Scout the field today. Set pheromone or sticky traps and follow integrated pest management on affected patches. Consult your local agriculture officer before spraying."
                    : "Warm, humid weather favours pests. Scout every 2 days, set traps, and avoid over-irrigation that keeps humidity high.";
            case SALINITY_STRESS -> red
                    ? "Flush soil with good-quality water, improve drainage, apply gypsum if the soil is sodic, and avoid saline irrigation water."
                    : "Soil salinity or pH is drifting out of range. Test irrigation water, improve drainage and review fertilizer choice within " + days + " day(s).";
            default -> "Continue regular monitoring.";
        };
    }

    // ------------------------------------------------------------------ helpers

    /** Least-squares slope (units per day) for a value series ordered by time. */
    static double slopePerDay(List<SensorReading> rs, Function<SensorReading, Double> f) {
        if (rs.size() < 2) return 0;
        LocalDateTime t0 = rs.get(0).getRecordedAt();
        List<double[]> pts = new ArrayList<>();
        for (SensorReading r : rs) {
            Double v = f.apply(r);
            if (v == null) continue;
            double x = Duration.between(t0, r.getRecordedAt()).toMinutes() / 1440.0;
            pts.add(new double[]{x, v});
        }
        if (pts.size() < 2) return 0;
        double n = pts.size(), sx = 0, sy = 0, sxy = 0, sxx = 0;
        for (double[] pt : pts) {
            sx += pt[0];
            sy += pt[1];
            sxy += pt[0] * pt[1];
            sxx += pt[0] * pt[0];
        }
        double den = n * sxx - sx * sx;
        if (Math.abs(den) < 1e-9) return 0;
        return (n * sxy - sx * sy) / den;
    }

    private static double nz(Double v, double d) { return v == null ? d : v; }
    private static double clamp01(double v) { return Math.max(0, Math.min(1, v)); }
    private static double round1(double v) { return Math.round(v * 10.0) / 10.0; }
    private static double round3(double v) { return Math.round(v * 1000.0) / 1000.0; }
}
