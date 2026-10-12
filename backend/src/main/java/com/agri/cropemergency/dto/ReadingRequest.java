package com.agri.cropemergency.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record ReadingRequest(
        LocalDateTime recordedAt,
        @NotNull(message = "Soil moisture is required") Double soilMoisture,
        Double soilPh,
        Double salinity,
        Double nitrogen,
        Double phosphorus,
        Double potassium,
        @NotNull(message = "NDVI is required") Double ndvi,
        @NotNull(message = "Temperature is required") Double temperature,
        @NotNull(message = "Humidity is required") Double humidity,
        Double rainfall,
        Double windSpeed) {
}
