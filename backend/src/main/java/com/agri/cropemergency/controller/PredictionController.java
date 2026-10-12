package com.agri.cropemergency.controller;

import com.agri.cropemergency.dto.FeedbackRequest;
import com.agri.cropemergency.model.FarmField;
import com.agri.cropemergency.model.Prediction;
import com.agri.cropemergency.repository.PredictionRepository;
import com.agri.cropemergency.service.FieldService;
import com.agri.cropemergency.service.PredictionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PredictionController {

    private final PredictionService predictionService;
    private final PredictionRepository predictions;
    private final FieldService fieldService;

    public PredictionController(PredictionService predictionService, PredictionRepository predictions,
                                FieldService fieldService) {
        this.predictionService = predictionService;
        this.predictions = predictions;
        this.fieldService = fieldService;
    }

    @PostMapping("/fields/{fieldId}/predict")
    @ResponseStatus(HttpStatus.CREATED)
    public Prediction predict(@PathVariable Long fieldId, Authentication auth) {
        FarmField field = fieldService.getAccessible(fieldId, auth);
        return predictionService.runPrediction(field);
    }

    @GetMapping("/fields/{fieldId}/predictions")
    public List<Prediction> history(@PathVariable Long fieldId,
                                    @RequestParam(defaultValue = "20") int limit,
                                    Authentication auth) {
        fieldService.getAccessible(fieldId, auth);
        return predictions.findByFieldIdOrderByCreatedAtDesc(fieldId, PageRequest.of(0, Math.max(1, Math.min(limit, 100))));
    }

    @PostMapping("/predictions/{id}/feedback")
    public Prediction feedback(@PathVariable Long id, @Valid @RequestBody FeedbackRequest req, Authentication auth) {
        Prediction p = predictions.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prediction not found"));
        fieldService.getAccessible(p.getField().getId(), auth);
        return predictionService.submitFeedback(p, req);
    }
}
