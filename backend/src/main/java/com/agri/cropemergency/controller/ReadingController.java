package com.agri.cropemergency.controller;

import com.agri.cropemergency.dto.ReadingRequest;
import com.agri.cropemergency.dto.SimulateRequest;
import com.agri.cropemergency.model.FarmField;
import com.agri.cropemergency.model.SensorReading;
import com.agri.cropemergency.repository.SensorReadingRepository;
import com.agri.cropemergency.service.FieldService;
import com.agri.cropemergency.service.SimulationService;
import com.agri.cropemergency.service.WeatherForecast;
import com.agri.cropemergency.service.WeatherService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fields/{fieldId}")
public class ReadingController {

    private final SensorReadingRepository readings;
    private final FieldService fieldService;
    private final SimulationService simulation;
    private final WeatherService weather;

    public ReadingController(SensorReadingRepository readings, FieldService fieldService,
                             SimulationService simulation, WeatherService weather) {
        this.readings = readings;
        this.fieldService = fieldService;
        this.simulation = simulation;
        this.weather = weather;
    }

    /** Oldest -> newest, ready for charts. */
    @GetMapping("/readings")
    public List<SensorReading> list(@PathVariable Long fieldId,
                                    @RequestParam(defaultValue = "30") int limit,
                                    Authentication auth) {
        fieldService.getAccessible(fieldId, auth);
        List<SensorReading> list = new ArrayList<>(readings.findByFieldIdOrderByRecordedAtDesc(
                fieldId, PageRequest.of(0, Math.max(1, Math.min(limit, 200)))));
        Collections.reverse(list);
        return list;
    }

    @PostMapping("/readings")
    @ResponseStatus(HttpStatus.CREATED)
    public SensorReading add(@PathVariable Long fieldId, @Valid @RequestBody ReadingRequest req, Authentication auth) {
        FarmField field = fieldService.getAccessible(fieldId, auth);
        SensorReading r = new SensorReading();
        r.setField(field);
        r.setRecordedAt(req.recordedAt() != null ? req.recordedAt() : LocalDateTime.now());
        r.setSoilMoisture(req.soilMoisture());
        r.setSoilPh(req.soilPh());
        r.setSalinity(req.salinity());
        r.setNitrogen(req.nitrogen());
        r.setPhosphorus(req.phosphorus());
        r.setPotassium(req.potassium());
        r.setNdvi(req.ndvi());
        r.setTemperature(req.temperature());
        r.setHumidity(req.humidity());
        r.setRainfall(req.rainfall());
        r.setWindSpeed(req.windSpeed());
        r.setSource("MANUAL");
        return readings.save(r);
    }

    @PostMapping("/simulate")
    public Map<String, Object> simulate(@PathVariable Long fieldId, @RequestBody SimulateRequest req, Authentication auth) {
        FarmField field = fieldService.getAccessible(fieldId, auth);
        int created = simulation.generate(field, req.scenario(), req.days() == null ? 10 : req.days());
        return Map.of("created", created, "scenario", req.scenario() == null ? "HEALTHY" : req.scenario().toUpperCase());
    }

    @GetMapping("/weather")
    public WeatherForecast weather(@PathVariable Long fieldId, Authentication auth) {
        FarmField f = fieldService.getAccessible(fieldId, auth);
        WeatherForecast wf = weather.forecast(f.getLatitude(), f.getLongitude());
        if (wf == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Weather service is currently unavailable");
        }
        return wf;
    }
}
