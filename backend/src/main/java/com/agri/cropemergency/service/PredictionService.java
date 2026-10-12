package com.agri.cropemergency.service;

import com.agri.cropemergency.dto.FeedbackRequest;
import com.agri.cropemergency.model.*;
import com.agri.cropemergency.repository.ModelWeightRepository;
import com.agri.cropemergency.repository.PredictionRepository;
import com.agri.cropemergency.repository.SensorReadingRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PredictionService {

    public record Accuracy(Double percent, int count) {
    }

    private final PredictionEngine engine;
    private final SensorReadingRepository readings;
    private final PredictionRepository predictions;
    private final ModelWeightRepository weights;
    private final WeatherService weather;
    private final AlertService alertService;
    private final ObjectMapper mapper;

    public PredictionService(PredictionEngine engine, SensorReadingRepository readings,
                             PredictionRepository predictions, ModelWeightRepository weights,
                             WeatherService weather, AlertService alertService, ObjectMapper mapper) {
        this.engine = engine;
        this.readings = readings;
        this.predictions = predictions;
        this.weights = weights;
        this.weather = weather;
        this.alertService = alertService;
        this.mapper = mapper;
    }

    @Transactional
    public Prediction runPrediction(FarmField field) {
        List<SensorReading> recent = new ArrayList<>(
                readings.findByFieldIdOrderByRecordedAtDesc(field.getId(), PageRequest.of(0, 10)));
        if (recent.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No data for this field yet. Add a reading or generate simulated data first.");
        }
        Collections.reverse(recent);   // oldest -> newest

        WeatherForecast wf = weather.forecast(field.getLatitude(), field.getLongitude());

        Map<String, Double> sensitivity = new HashMap<>();
        weights.findAll().forEach(w -> sensitivity.put(w.getThreat(), w.getSensitivity()));

        PredictionEngine.Result r = engine.evaluate(field, recent, wf, sensitivity);

        Prediction p = new Prediction();
        p.setField(field);
        p.setThreat(r.threat());
        p.setRiskLevel(r.risk());
        p.setRiskScore(r.score());
        p.setSeverity(r.severity());
        p.setDaysToImpact(r.daysToImpact());
        p.setMoistureDeficit(r.moistureDeficit());
        p.setHeatStressIndex(r.heatStressIndex());
        p.setNdviDeclineRate(r.ndviDeclineRate());
        p.setForecastMaxTemp(wf != null ? wf.maxTemp() : null);
        p.setForecastRain3d(wf != null ? wf.rain3d() : null);
        p.setRecommendation(PredictionEngine.recommend(r.threat(), r.risk(), r.daysToImpact()));
        try {
            p.setScoresJson(mapper.writeValueAsString(r.scores()));
        } catch (JsonProcessingException e) {
            p.setScoresJson("{}");
        }
        Prediction saved = predictions.save(p);

        if (saved.getRiskLevel() != RiskLevel.GREEN) {
            alertService.createFor(saved);
        }
        return saved;
    }

    /** Farmer reports what really happened -> model sensitivity is adjusted (continuous learning). */
    @Transactional
    public Prediction submitFeedback(Prediction p, FeedbackRequest req) {
        if (req.outcome() == Outcome.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose PROBLEM_OCCURRED or NO_PROBLEM");
        }
        if (p.getOutcome() != Outcome.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Feedback was already submitted for this prediction");
        }
        p.setOutcome(req.outcome());
        p.setActualThreat(req.actualThreat());
        p.setFeedbackNote(req.note());
        p.setFeedbackAt(LocalDateTime.now());

        boolean flagged = p.getRiskLevel() != RiskLevel.GREEN;
        if (flagged && req.outcome() == Outcome.PROBLEM_OCCURRED) {
            ModelWeight w = weight(p.getThreat().name());
            w.setConfirmedCount(w.getConfirmedCount() + 1);
            w.setSensitivity(Math.min(1.5, w.getSensitivity() + 0.03));
            weights.save(w);
        } else if (flagged && req.outcome() == Outcome.NO_PROBLEM) {
            ModelWeight w = weight(p.getThreat().name());
            w.setFalseAlarmCount(w.getFalseAlarmCount() + 1);
            w.setSensitivity(Math.max(0.6, w.getSensitivity() - 0.05));
            weights.save(w);
        } else if (!flagged && req.outcome() == Outcome.PROBLEM_OCCURRED) {
            Threat actual = parseThreat(req.actualThreat());
            if (actual != null) {
                ModelWeight w = weight(actual.name());
                w.setMissedCount(w.getMissedCount() + 1);
                w.setSensitivity(Math.min(1.5, w.getSensitivity() + 0.08));
                weights.save(w);
            }
        }
        return predictions.save(p);
    }

    public Accuracy accuracy(List<Prediction> withFeedback) {
        if (withFeedback.isEmpty()) return new Accuracy(null, 0);
        long correct = withFeedback.stream().filter(p -> {
            boolean flagged = p.getRiskLevel() != RiskLevel.GREEN;
            boolean happened = p.getOutcome() == Outcome.PROBLEM_OCCURRED;
            return flagged == happened;
        }).count();
        double pct = Math.round(correct * 1000.0 / withFeedback.size()) / 10.0;
        return new Accuracy(pct, withFeedback.size());
    }

    private ModelWeight weight(String threat) {
        return weights.findById(threat).orElseGet(() -> new ModelWeight(threat));
    }

    private Threat parseThreat(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            Threat t = Threat.valueOf(s.trim().toUpperCase());
            return t == Threat.NONE ? null : t;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
