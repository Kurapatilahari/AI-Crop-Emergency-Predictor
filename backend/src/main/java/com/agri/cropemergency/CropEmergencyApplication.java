package com.agri.cropemergency;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CropEmergencyApplication {
    public static void main(String[] args) {
        SpringApplication.run(CropEmergencyApplication.class, args);
    }
}
