package com.agri.cropemergency.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Locale;

/** Pulls the free Open-Meteo forecast (no API key). Returns null when unavailable. */
@Service
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

    private final RestTemplate rest;
    private final boolean enabled;

    public WeatherService(@Value("${app.weather.enabled:true}") boolean enabled) {
        this.enabled = enabled;
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(3000);
        f.setReadTimeout(4000);
        this.rest = new RestTemplate(f);
    }

    public WeatherForecast forecast(Double lat, Double lon) {
        if (!enabled || lat == null || lon == null) return null;
        try {
            String url = String.format(Locale.US,
                    "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f"
                            + "&daily=temperature_2m_max,temperature_2m_min,precipitation_sum,wind_speed_10m_max"
                            + "&hourly=relative_humidity_2m&forecast_days=3&timezone=auto", lat, lon);
            JsonNode root = rest.getForObject(url, JsonNode.class);
            if (root == null) return null;

            JsonNode daily = root.path("daily");
            double maxT = max(daily.path("temperature_2m_max"), Double.NaN);
            double minT = min(daily.path("temperature_2m_min"));
            double rain = sum(daily.path("precipitation_sum"));
            double wind = max(daily.path("wind_speed_10m_max"), 0);
            double hum = avg(root.path("hourly").path("relative_humidity_2m"));
            if (Double.isNaN(maxT)) return null;
            return new WeatherForecast(round(maxT), round(minT), round(rain), round(wind), round(hum), "Open-Meteo");
        } catch (Exception e) {
            log.warn("Weather forecast unavailable: {}", e.getMessage());
            return null;
        }
    }

    private static double max(JsonNode arr, double fallback) {
        double m = Double.NEGATIVE_INFINITY;
        for (JsonNode n : arr) if (n.isNumber()) m = Math.max(m, n.asDouble());
        return m == Double.NEGATIVE_INFINITY ? fallback : m;
    }

    private static double min(JsonNode arr) {
        double m = Double.POSITIVE_INFINITY;
        for (JsonNode n : arr) if (n.isNumber()) m = Math.min(m, n.asDouble());
        return m == Double.POSITIVE_INFINITY ? 0 : m;
    }

    private static double sum(JsonNode arr) {
        double s = 0;
        for (JsonNode n : arr) if (n.isNumber()) s += n.asDouble();
        return s;
    }

    private static double avg(JsonNode arr) {
        double s = 0;
        int c = 0;
        for (JsonNode n : arr) if (n.isNumber()) { s += n.asDouble(); c++; }
        return c == 0 ? 0 : s / c;
    }

    private static double round(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
