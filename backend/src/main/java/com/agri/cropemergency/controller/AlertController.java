package com.agri.cropemergency.controller;

import com.agri.cropemergency.model.Alert;
import com.agri.cropemergency.repository.AlertRepository;
import com.agri.cropemergency.service.FieldService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertRepository alerts;
    private final FieldService fieldService;

    public AlertController(AlertRepository alerts, FieldService fieldService) {
        this.alerts = alerts;
        this.fieldService = fieldService;
    }

    @GetMapping
    public List<Alert> list(Authentication auth) {
        Long uid = fieldService.currentUser(auth).getId();
        return alerts.findByUserIdOrderByCreatedAtDesc(uid, PageRequest.of(0, 100));
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unread(Authentication auth) {
        Long uid = fieldService.currentUser(auth).getId();
        return Map.of("count", alerts.countByUserIdAndSeenFalse(uid));
    }

    @PostMapping("/{id}/read")
    public Alert markRead(@PathVariable Long id, Authentication auth) {
        Long uid = fieldService.currentUser(auth).getId();
        Alert a = alerts.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found"));
        if (!a.getUserId().equals(uid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your alert");
        }
        a.setSeen(true);
        return alerts.save(a);
    }

    @PostMapping("/read-all")
    public Map<String, Integer> markAll(Authentication auth) {
        Long uid = fieldService.currentUser(auth).getId();
        List<Alert> unseen = alerts.findByUserIdAndSeenFalse(uid);
        unseen.forEach(a -> a.setSeen(true));
        alerts.saveAll(unseen);
        return Map.of("updated", unseen.size());
    }
}
