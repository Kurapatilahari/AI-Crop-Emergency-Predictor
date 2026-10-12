package com.agri.cropemergency.service;

import com.agri.cropemergency.model.Alert;
import com.agri.cropemergency.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Delivery channels. In-app alerts are always stored; SMS / voice are logged here.
 * Plug a provider (Twilio, MSG91, Exotel...) into sendSms / sendVoice to go live.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Value("${app.notifications.sms-enabled:false}")
    private boolean smsEnabled;

    public void dispatch(User user, Alert alert) {
        String channels = alert.getChannels() == null ? "" : alert.getChannels();
        if (channels.contains("SMS")) sendSms(user.getPhone(), alert.getMessage());
        if (channels.contains("VOICE")) sendVoice(user.getPhone(), user.getLanguage(), alert.getMessage());
    }

    private void sendSms(String phone, String message) {
        log.info("[SMS{} -> {}] {}", smsEnabled ? "" : " (simulated)", phone, message);
        // TODO: call SMS provider API when smsEnabled = true
    }

    private void sendVoice(String phone, String language, String message) {
        log.info("[VOICE{} ({}) -> {}] {}", smsEnabled ? "" : " (simulated)", language, phone, message);
        // TODO: call text-to-speech / voice-call provider
    }
}
