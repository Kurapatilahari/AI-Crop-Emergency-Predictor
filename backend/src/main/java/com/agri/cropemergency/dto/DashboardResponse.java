package com.agri.cropemergency.dto;

import java.util.List;

public record DashboardResponse(
        int totalFields,
        int green,
        int amber,
        int red,
        int unknown,
        long unreadAlerts,
        Double accuracy,
        int feedbackCount,
        List<FieldSummary> fields) {

    public record FieldSummary(
            Long fieldId,
            String name,
            String cropType,
            String riskLevel,
            String threat,
            Double score,
            Integer daysToImpact,
            String recommendation) {
    }
}
