package com.agri.cropemergency.controller;

import com.agri.cropemergency.dto.UserDto;
import com.agri.cropemergency.model.ModelWeight;
import com.agri.cropemergency.model.Outcome;
import com.agri.cropemergency.model.Role;
import com.agri.cropemergency.model.User;
import com.agri.cropemergency.repository.*;
import com.agri.cropemergency.service.FieldService;
import com.agri.cropemergency.service.PredictionService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository users;
    private final FarmFieldRepository fields;
    private final PredictionRepository predictions;
    private final AlertRepository alerts;
    private final ModelWeightRepository weights;
    private final FieldService fieldService;
    private final PredictionService predictionService;

    public AdminController(UserRepository users, FarmFieldRepository fields, PredictionRepository predictions,
                           AlertRepository alerts, ModelWeightRepository weights, FieldService fieldService,
                           PredictionService predictionService) {
        this.users = users;
        this.fields = fields;
        this.predictions = predictions;
        this.alerts = alerts;
        this.weights = weights;
        this.fieldService = fieldService;
        this.predictionService = predictionService;
    }

    @GetMapping("/users")
    public List<UserDto> users() {
        return users.findAll().stream().map(UserDto::from).toList();
    }

    @DeleteMapping("/users/{id}")
    @Transactional
    public void deleteUser(@PathVariable Long id, Authentication auth) {
        User u = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (u.getRole() == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin accounts cannot be deleted");
        }
        fields.findByOwnerIdOrderByIdDesc(id).forEach(fieldService::deleteCascade);
        users.delete(u);
    }

    @GetMapping("/model")
    public List<ModelWeight> model() {
        return weights.findAll();
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        PredictionService.Accuracy acc = predictionService.accuracy(predictions.findByOutcomeNot(Outcome.PENDING));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("users", users.count());
        m.put("fields", fields.count());
        m.put("predictions", predictions.count());
        m.put("alerts", alerts.count());
        m.put("feedbackCount", acc.count());
        m.put("accuracy", acc.percent());
        return m;
    }
}
