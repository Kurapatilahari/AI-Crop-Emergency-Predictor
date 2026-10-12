package com.agri.cropemergency.service;

import com.agri.cropemergency.model.FarmField;
import com.agri.cropemergency.repository.FarmFieldRepository;
import com.agri.cropemergency.repository.SensorReadingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Continuously monitors every field: runs the prediction engine on a schedule. */
@Component
public class PredictionScheduler {

    private static final Logger log = LoggerFactory.getLogger(PredictionScheduler.class);

    private final FarmFieldRepository fields;
    private final SensorReadingRepository readings;
    private final PredictionService predictionService;

    public PredictionScheduler(FarmFieldRepository fields, SensorReadingRepository readings,
                               PredictionService predictionService) {
        this.fields = fields;
        this.readings = readings;
        this.predictionService = predictionService;
    }

    @Scheduled(cron = "${app.scheduler.cron:0 0 6 * * *}")
    public void runDaily() {
        log.info("Scheduled prediction run started");
        int ok = 0;
        for (FarmField f : fields.findAll()) {
            try {
                if (readings.countByFieldId(f.getId()) > 0) {
                    predictionService.runPrediction(f);
                    ok++;
                }
            } catch (Exception e) {
                log.warn("Prediction failed for field {}: {}", f.getId(), e.getMessage());
            }
        }
        log.info("Scheduled prediction run finished: {} field(s)", ok);
    }
}
