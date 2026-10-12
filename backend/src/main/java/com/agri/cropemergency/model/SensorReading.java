package com.agri.cropemergency.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/** One fused data point for a field: soil sensors + weather + vegetation index. */
@Entity
@Table(name = "sensor_readings", indexes = @Index(name = "idx_reading_field_time", columnList = "field_id,recorded_at"))
public class SensorReading {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "field_id")
    private FarmField field;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt = LocalDateTime.now();

    private Double soilMoisture;   // % volumetric
    private Double soilPh;
    private Double salinity;       // dS/m
    private Double nitrogen;       // kg/ha
    private Double phosphorus;     // kg/ha
    private Double potassium;      // kg/ha
    private Double ndvi;           // 0..1
    private Double temperature;    // deg C
    private Double humidity;       // %
    private Double rainfall;       // mm (last 24h)
    private Double windSpeed;      // km/h

    /** MANUAL, SIMULATED, SENSOR */
    private String source = "MANUAL";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public FarmField getField() { return field; }
    public void setField(FarmField field) { this.field = field; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
    public Double getSoilMoisture() { return soilMoisture; }
    public void setSoilMoisture(Double soilMoisture) { this.soilMoisture = soilMoisture; }
    public Double getSoilPh() { return soilPh; }
    public void setSoilPh(Double soilPh) { this.soilPh = soilPh; }
    public Double getSalinity() { return salinity; }
    public void setSalinity(Double salinity) { this.salinity = salinity; }
    public Double getNitrogen() { return nitrogen; }
    public void setNitrogen(Double nitrogen) { this.nitrogen = nitrogen; }
    public Double getPhosphorus() { return phosphorus; }
    public void setPhosphorus(Double phosphorus) { this.phosphorus = phosphorus; }
    public Double getPotassium() { return potassium; }
    public void setPotassium(Double potassium) { this.potassium = potassium; }
    public Double getNdvi() { return ndvi; }
    public void setNdvi(Double ndvi) { this.ndvi = ndvi; }
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    public Double getHumidity() { return humidity; }
    public void setHumidity(Double humidity) { this.humidity = humidity; }
    public Double getRainfall() { return rainfall; }
    public void setRainfall(Double rainfall) { this.rainfall = rainfall; }
    public Double getWindSpeed() { return windSpeed; }
    public void setWindSpeed(Double windSpeed) { this.windSpeed = windSpeed; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
