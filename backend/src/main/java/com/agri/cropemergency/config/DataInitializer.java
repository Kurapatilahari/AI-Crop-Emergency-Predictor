package com.agri.cropemergency.config;

import com.agri.cropemergency.model.*;
import com.agri.cropemergency.repository.FarmFieldRepository;
import com.agri.cropemergency.repository.ModelWeightRepository;
import com.agri.cropemergency.repository.UserRepository;
import com.agri.cropemergency.service.PredictionService;
import com.agri.cropemergency.service.SimulationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Seeds model weights, an admin account and a demo farmer on first run. */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository users;
    private final FarmFieldRepository fields;
    private final ModelWeightRepository weights;
    private final PasswordEncoder encoder;
    private final SimulationService simulation;
    private final PredictionService predictionService;

    public DataInitializer(UserRepository users, FarmFieldRepository fields, ModelWeightRepository weights,
                           PasswordEncoder encoder, SimulationService simulation, PredictionService predictionService) {
        this.users = users;
        this.fields = fields;
        this.weights = weights;
        this.encoder = encoder;
        this.simulation = simulation;
        this.predictionService = predictionService;
    }

    @Override
    public void run(String... args) {
        for (Threat t : Threat.values()) {
            if (t != Threat.NONE && !weights.existsById(t.name())) {
                weights.save(new ModelWeight(t.name()));
            }
        }

        if (!users.existsByEmail("admin@agri.com")) {
            User admin = new User();
            admin.setName("System Admin");
            admin.setEmail("admin@agri.com");
            admin.setPassword(encoder.encode("Admin@123"));
            admin.setRole(Role.ADMIN);
            admin.setPhone("+910000000000");
            users.save(admin);
            log.info("Created admin account admin@agri.com / Admin@123");
        }

        if (!users.existsByEmail("farmer@agri.com")) {
            User farmer = new User();
            farmer.setName("Ravi Kumar");
            farmer.setEmail("farmer@agri.com");
            farmer.setPassword(encoder.encode("Farmer@123"));
            farmer.setRole(Role.FARMER);
            farmer.setPhone("+919876543210");
            farmer.setLanguage("en");
            users.save(farmer);

            demoField(farmer, "North Paddy", "Rice", 17.3850, 78.4867, 4.5, "HEALTHY", 1.0);
            demoField(farmer, "South Cotton Plot", "Cotton", 17.4000, 78.5000, 6.0, "DROUGHT", 0.8);
            demoField(farmer, "Tomato Greenhouse", "Tomato", 17.3700, 78.4700, 1.2, "PEST", 1.0);
            log.info("Created demo farmer farmer@agri.com / Farmer@123 with 3 demo fields");
        }
    }

    private void demoField(User owner, String name, String crop, double lat, double lon, double area,
                           String scenario, double intensity) {
        FarmField f = new FarmField();
        f.setOwner(owner);
        f.setName(name);
        f.setCropType(crop);
        f.setLatitude(lat);
        f.setLongitude(lon);
        f.setAreaAcres(area);
        f = fields.save(f);
        try {
            simulation.generate(f, scenario, 10, intensity);
            predictionService.runPrediction(f);
        } catch (Exception e) {
            log.warn("Could not create demo prediction for {}: {}", name, e.getMessage());
        }
    }
}
