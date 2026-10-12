package com.agri.cropemergency.service;

import java.util.LinkedHashMap;
import java.util.Map;

/** Agronomic thresholds per crop used by the prediction engine. */
public final class CropProfiles {

    public record CropProfile(String name,
                              double minMoisture, double maxMoisture,   // % volumetric soil moisture
                              double heatThreshold,                     // deg C
                              double phMin, double phMax,
                              double minN, double minP, double minK) {   // kg/ha
    }

    private static final Map<String, CropProfile> PROFILES = new LinkedHashMap<>();

    static {
        add(new CropProfile("Rice",      40, 85, 35, 5.5, 7.0, 280, 20, 150));
        add(new CropProfile("Wheat",     20, 40, 32, 6.0, 7.5, 280, 22, 140));
        add(new CropProfile("Maize",     22, 42, 35, 5.8, 7.0, 280, 22, 140));
        add(new CropProfile("Cotton",    20, 40, 38, 6.0, 8.0, 250, 20, 130));
        add(new CropProfile("Tomato",    25, 45, 33, 6.0, 6.8, 250, 25, 150));
        add(new CropProfile("Sugarcane", 30, 55, 38, 6.5, 7.5, 280, 22, 150));
        add(new CropProfile("Groundnut", 18, 35, 36, 5.5, 7.0, 220, 20, 120));
        add(new CropProfile("Chilli",    22, 42, 34, 6.0, 7.0, 250, 22, 140));
    }

    private static final CropProfile DEFAULT = new CropProfile("Generic", 22, 42, 35, 6.0, 7.5, 250, 20, 130);

    private CropProfiles() {}

    private static void add(CropProfile p) {
        PROFILES.put(p.name().toLowerCase(), p);
    }

    public static CropProfile forCrop(String crop) {
        if (crop == null) return DEFAULT;
        return PROFILES.getOrDefault(crop.trim().toLowerCase(), DEFAULT);
    }

    public static java.util.List<String> names() {
        return PROFILES.values().stream().map(CropProfile::name).toList();
    }
}
