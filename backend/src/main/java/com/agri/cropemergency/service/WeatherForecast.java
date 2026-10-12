package com.agri.cropemergency.service;

/** 3-day weather outlook summary. */
public record WeatherForecast(double maxTemp, double minTemp, double rain3d, double maxWind,
                              double avgHumidity, String source) {
}
