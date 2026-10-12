package com.agri.cropemergency.service;

import com.agri.cropemergency.model.FarmField;
import com.agri.cropemergency.model.SensorReading;
import com.agri.cropemergency.repository.SensorReadingRepository;
import com.agri.cropemergency.service.CropProfiles.CropProfile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates realistic sensor / NDVI history so the system can be demonstrated without hardware.
 * Scenarios: HEALTHY, DROUGHT, HEAT, WATERLOGGING, NUTRIENT, PEST, SALINITY.
 */
@Service
public class SimulationService {

    private final SensorReadingRepository repo;
    private final Random rnd = new Random();

    public SimulationService(SensorReadingRepository repo) {
        this.repo = repo;
    }

    public int generate(FarmField field, String scenario, int days) {
        return generate(field, scenario, days, 1.0);
    }

    /** intensity 0..1 controls how far the scenario develops (1.0 = full emergency, ~0.5 = early warning). */
    @Transactional
    public int generate(FarmField field, String scenario, int days, double intensity) {
        CropProfile p = CropProfiles.forCrop(field.getCropType());
        String sc = scenario == null ? "HEALTHY" : scenario.trim().toUpperCase();
        days = Math.max(3, Math.min(30, days));

        repo.deleteByFieldIdAndSource(field.getId(), "SIMULATED");

        double midM = (p.minMoisture() + p.maxMoisture()) / 2;
        double midPh = (p.phMin() + p.phMax()) / 2;
        LocalDateTime now = LocalDateTime.now();
        List<SensorReading> list = new ArrayList<>();

        for (int i = 0; i < days; i++) {
            double t = (double) i / (days - 1) * Math.max(0, Math.min(1, intensity));

            double moisture = midM;
            double temp = p.heatThreshold() - 6;
            double hum = 58;
            double ndvi = 0.72;
            double ph = midPh;
            double sal = 1.2;
            double n = p.minN() * 1.35, ph2 = p.minP() * 1.35, k = p.minK() * 1.35;
            double rain = 0;
            double wind = 10;

            switch (sc) {
                case "DROUGHT" -> {
                    moisture = lerp(midM, p.minMoisture() * 0.55, t);
                    temp += lerp(0, 5, t);
                    hum = lerp(55, 38, t);
                    ndvi = lerp(0.72, 0.56, t);
                }
                case "HEAT" -> {
                    temp = lerp(p.heatThreshold() - 4, p.heatThreshold() + 6, t);
                    hum = lerp(60, 50, t);
                    ndvi = lerp(0.72, 0.60, t);
                    moisture = lerp(midM, p.minMoisture() * 1.05, t);
                }
                case "WATERLOGGING" -> {
                    moisture = lerp(midM, p.maxMoisture() + (100 - p.maxMoisture()) * 0.55, t);
                    rain = lerp(5, 55, t);
                    hum = lerp(70, 92, t);
                    ndvi = lerp(0.72, 0.63, t);
                    temp -= 2;
                }
                case "NUTRIENT" -> {
                    double f = lerp(1.3, 0.45, t);
                    n = p.minN() * f;
                    ph2 = p.minP() * f;
                    k = p.minK() * f;
                    ndvi = lerp(0.72, 0.50, t);
                }
                case "PEST" -> {
                    temp = 29;
                    hum = lerp(68, 90, t);
                    ndvi = lerp(0.72, 0.52, t);
                }
                case "SALINITY" -> {
                    sal = lerp(1.4, 5.5, t);
                    ph = lerp(midPh, p.phMax() + 1.3, t);
                    ndvi = lerp(0.72, 0.62, t);
                }
                default -> { /* HEALTHY */ }
            }

            SensorReading r = new SensorReading();
            r.setField(field);
            r.setSource("SIMULATED");
            r.setRecordedAt(now.minusDays(days - 1 - i));
            r.setSoilMoisture(round(clamp(moisture + noise(1.0), 3, 98)));
            r.setTemperature(round(temp + noise(0.8)));
            r.setHumidity(round(clamp(hum + noise(2.0), 10, 100)));
            r.setNdvi(round2(clamp(ndvi + noise(0.01), 0.05, 0.95)));
            r.setSoilPh(round(clamp(ph + noise(0.05), 3, 10)));
            r.setSalinity(round(Math.max(0, sal + noise(0.1))));
            r.setNitrogen(round(Math.max(0, n + noise(3))));
            r.setPhosphorus(round(Math.max(0, ph2 + noise(0.5))));
            r.setPotassium(round(Math.max(0, k + noise(2))));
            r.setRainfall(round(Math.max(0, rain + (rain > 0 ? noise(3) : 0))));
            r.setWindSpeed(round(Math.max(0, wind + noise(3))));
            list.add(r);
        }
        repo.saveAll(list);
        return list.size();
    }

    private double noise(double amp) { return (rnd.nextDouble() - 0.5) * 2 * amp; }
    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }
    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }
    private static double round(double v) { return Math.round(v * 10.0) / 10.0; }
    private static double round2(double v) { return Math.round(v * 100.0) / 100.0; }
}
