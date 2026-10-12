package com.agri.cropemergency.controller;

import com.agri.cropemergency.dto.DashboardResponse;
import com.agri.cropemergency.dto.DashboardResponse.FieldSummary;
import com.agri.cropemergency.model.FarmField;
import com.agri.cropemergency.model.Outcome;
import com.agri.cropemergency.model.Prediction;
import com.agri.cropemergency.model.User;
import com.agri.cropemergency.repository.AlertRepository;
import com.agri.cropemergency.repository.FarmFieldRepository;
import com.agri.cropemergency.repository.PredictionRepository;
import com.agri.cropemergency.service.FieldService;
import com.agri.cropemergency.service.PredictionService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final FieldService fieldService;
    private final FarmFieldRepository fields;
    private final PredictionRepository predictions;
    private final AlertRepository alerts;
    private final PredictionService predictionService;

    public DashboardController(FieldService fieldService, FarmFieldRepository fields, PredictionRepository predictions,
                               AlertRepository alerts, PredictionService predictionService) {
        this.fieldService = fieldService;
        this.fields = fields;
        this.predictions = predictions;
        this.alerts = alerts;
        this.predictionService = predictionService;
    }

    @GetMapping
    public DashboardResponse dashboard(Authentication auth) {
        User user = fieldService.currentUser(auth);
        List<FarmField> myFields = fields.findByOwnerIdOrderByIdDesc(user.getId());

        int green = 0, amber = 0, red = 0, unknown = 0;
        List<FieldSummary> summaries = new ArrayList<>();
        for (FarmField f : myFields) {
            Optional<Prediction> latest = predictions.findFirstByFieldIdOrderByCreatedAtDesc(f.getId());
            if (latest.isEmpty()) {
                unknown++;
                summaries.add(new FieldSummary(f.getId(), f.getName(), f.getCropType(), "UNKNOWN", null, null, null,
                        "No prediction yet - add data and run the AI engine."));
                continue;
            }
            Prediction p = latest.get();
            switch (p.getRiskLevel()) {
                case GREEN -> green++;
                case AMBER -> amber++;
                case RED -> red++;
            }
            summaries.add(new FieldSummary(f.getId(), f.getName(), f.getCropType(), p.getRiskLevel().name(),
                    p.getThreat().name(), p.getRiskScore(), p.getDaysToImpact(), p.getRecommendation()));
        }

        PredictionService.Accuracy acc = predictionService.accuracy(
                predictions.findByFieldOwnerIdAndOutcomeNot(user.getId(), Outcome.PENDING));

        return new DashboardResponse(myFields.size(), green, amber, red, unknown,
                alerts.countByUserIdAndSeenFalse(user.getId()), acc.percent(), acc.count(), summaries);
    }
}
