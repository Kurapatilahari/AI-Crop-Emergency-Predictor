package com.agri.cropemergency.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FieldRequest(
        @NotBlank(message = "Field name is required") String name,
        @NotBlank(message = "Crop type is required") String cropType,
        @NotNull(message = "Latitude is required") Double latitude,
        @NotNull(message = "Longitude is required") Double longitude,
        Double areaAcres) {
}
