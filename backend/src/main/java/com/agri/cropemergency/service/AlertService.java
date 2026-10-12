package com.agri.cropemergency.service;

import com.agri.cropemergency.model.*;
import com.agri.cropemergency.repository.AlertRepository;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AlertService {

    private static final Map<String, Map<String, String>> THREAT_NAMES = Map.of(
            "hi", Map.of(
                    "DROUGHT", "सूखा", "HEAT_STRESS", "गर्मी का तनाव", "WATERLOGGING", "जलभराव",
                    "NUTRIENT_DEFICIENCY", "पोषक तत्वों की कमी", "PEST_OUTBREAK", "कीट प्रकोप",
                    "SALINITY_STRESS", "लवणता तनाव"),
            "te", Map.of(
                    "DROUGHT", "కరువు", "HEAT_STRESS", "వేడి ఒత్తిడి", "WATERLOGGING", "నీటి ముంపు",
                    "NUTRIENT_DEFICIENCY", "పోషకాల లోపం", "PEST_OUTBREAK", "చీడపీడల ఉధృతి",
                    "SALINITY_STRESS", "లవణీయత ఒత్తిడి"));

    private final AlertRepository alerts;
    private final NotificationService notifications;

    public AlertService(AlertRepository alerts, NotificationService notifications) {
        this.alerts = alerts;
        this.notifications = notifications;
    }

    public Alert createFor(Prediction p) {
        User user = p.getField().getOwner();
        boolean red = p.getRiskLevel() == RiskLevel.RED;

        Alert a = new Alert();
        a.setUserId(user.getId());
        a.setFieldId(p.getField().getId());
        a.setFieldName(p.getField().getName());
        a.setPredictionId(p.getId());
        a.setRiskLevel(p.getRiskLevel());
        a.setChannels(red ? "APP,SMS,VOICE" : "APP");
        a.setMessage(buildMessage(user.getLanguage(), p, red));
        Alert saved = alerts.save(a);
        notifications.dispatch(user, saved);
        return saved;
    }

    private String buildMessage(String lang, Prediction p, boolean red) {
        String field = p.getField().getName();
        int days = p.getDaysToImpact() == null ? 0 : p.getDaysToImpact();
        String threatKey = p.getThreat().name();

        if ("hi".equals(lang)) {
            String t = THREAT_NAMES.get("hi").getOrDefault(threatKey, threatKey);
            return red
                    ? "रेड अलर्ट: " + field + " में " + t + " का खतरा, लगभग " + days + " दिन में। तुरंत कार्रवाई करें।"
                    : "एम्बर चेतावनी: " + field + " में " + t + " का खतरा, लगभग " + days + " दिन में। ध्यान से निगरानी करें।";
        }
        if ("te".equals(lang)) {
            String t = THREAT_NAMES.get("te").getOrDefault(threatKey, threatKey);
            return red
                    ? "రెడ్ అలర్ట్: " + field + " లో " + t + " ప్రమాదం, సుమారు " + days + " రోజుల్లో. వెంటనే చర్య తీసుకోండి."
                    : "అంబర్ హెచ్చరిక: " + field + " లో " + t + " ప్రమాదం, సుమారు " + days + " రోజుల్లో. జాగ్రత్తగా గమనించండి.";
        }
        String label = threatKey.replace('_', ' ').toLowerCase();
        return (red ? "RED ALERT" : "AMBER WATCH") + ": " + label + " risk in '" + field + "' (score "
                + Math.round(p.getRiskScore()) + "/100), expected in about " + days + " day(s). "
                + p.getRecommendation();
    }
}
