package com.agri.cropemergency.service;

import com.agri.cropemergency.model.FarmField;
import com.agri.cropemergency.model.User;
import com.agri.cropemergency.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FieldService {

    private final UserRepository users;
    private final FarmFieldRepository fields;
    private final SensorReadingRepository readings;
    private final PredictionRepository predictions;
    private final AlertRepository alerts;

    public FieldService(UserRepository users, FarmFieldRepository fields, SensorReadingRepository readings,
                        PredictionRepository predictions, AlertRepository alerts) {
        this.users = users;
        this.fields = fields;
        this.readings = readings;
        this.predictions = predictions;
        this.alerts = alerts;
    }

    public User currentUser(Authentication auth) {
        return users.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    public boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    /** Returns the field if the caller owns it (or is an admin). */
    public FarmField getAccessible(Long id, Authentication auth) {
        FarmField f = fields.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Field not found"));
        if (!isAdmin(auth) && !f.getOwner().getEmail().equals(auth.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this field");
        }
        return f;
    }

    @Transactional
    public void deleteCascade(FarmField f) {
        alerts.deleteByFieldId(f.getId());
        predictions.deleteByFieldId(f.getId());
        readings.deleteByFieldId(f.getId());
        fields.delete(f);
    }
}
