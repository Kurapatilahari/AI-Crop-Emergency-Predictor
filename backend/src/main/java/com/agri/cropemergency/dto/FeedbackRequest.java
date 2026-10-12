package com.agri.cropemergency.dto;

import com.agri.cropemergency.model.Outcome;
import jakarta.validation.constraints.NotNull;

public record FeedbackRequest(
        @NotNull(message = "Outcome is required") Outcome outcome,
        String actualThreat,
        String note) {
}
