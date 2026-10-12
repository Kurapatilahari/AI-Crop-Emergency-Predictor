package com.agri.cropemergency.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "predictions")
public class Prediction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "field_id")
    private FarmField field;

    @Enumerated(EnumType.STRING)
    private Threat threat;

    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    private Double riskScore;          // 0..100
    private String severity;           // LOW / MODERATE / SEVERE / CRITICAL
    private Integer daysToImpact;      // "when" the threat is likely to occur

    // derived stress indicators
    private Double moistureDeficit;    // 0..1 ratio below crop minimum
    private Double heatStressIndex;    // 0..1
    private Double ndviDeclineRate;    // NDVI units per day

    private Double forecastMaxTemp;
    private Double forecastRain3d;

    @Column(length = 1500)
    private String recommendation;

    @Column(length = 2000)
    private String scoresJson;         // per-threat scores

    private LocalDateTime createdAt = LocalDateTime.now();

    // feedback loop
    @Enumerated(EnumType.STRING)
    private Outcome outcome = Outcome.PENDING;
    private String actualThreat;
    @Column(length = 500)
    private String feedbackNote;
    private LocalDateTime feedbackAt;

    @JsonProperty("fieldId")
    public Long fieldId() { return field != null ? field.getId() : null; }

    @JsonProperty("fieldName")
    public String fieldName() { return field != null ? field.getName() : null; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public FarmField getField() { return field; }
    public void setField(FarmField field) { this.field = field; }
    public Threat getThreat() { return threat; }
    public void setThreat(Threat threat) { this.threat = threat; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }
    public Double getRiskScore() { return riskScore; }
    public void setRiskScore(Double riskScore) { this.riskScore = riskScore; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public Integer getDaysToImpact() { return daysToImpact; }
    public void setDaysToImpact(Integer daysToImpact) { this.daysToImpact = daysToImpact; }
    public Double getMoistureDeficit() { return moistureDeficit; }
    public void setMoistureDeficit(Double moistureDeficit) { this.moistureDeficit = moistureDeficit; }
    public Double getHeatStressIndex() { return heatStressIndex; }
    public void setHeatStressIndex(Double heatStressIndex) { this.heatStressIndex = heatStressIndex; }
    public Double getNdviDeclineRate() { return ndviDeclineRate; }
    public void setNdviDeclineRate(Double ndviDeclineRate) { this.ndviDeclineRate = ndviDeclineRate; }
    public Double getForecastMaxTemp() { return forecastMaxTemp; }
    public void setForecastMaxTemp(Double forecastMaxTemp) { this.forecastMaxTemp = forecastMaxTemp; }
    public Double getForecastRain3d() { return forecastRain3d; }
    public void setForecastRain3d(Double forecastRain3d) { this.forecastRain3d = forecastRain3d; }
    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
    public String getScoresJson() { return scoresJson; }
    public void setScoresJson(String scoresJson) { this.scoresJson = scoresJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Outcome getOutcome() { return outcome; }
    public void setOutcome(Outcome outcome) { this.outcome = outcome; }
    public String getActualThreat() { return actualThreat; }
    public void setActualThreat(String actualThreat) { this.actualThreat = actualThreat; }
    public String getFeedbackNote() { return feedbackNote; }
    public void setFeedbackNote(String feedbackNote) { this.feedbackNote = feedbackNote; }
    public LocalDateTime getFeedbackAt() { return feedbackAt; }
    public void setFeedbackAt(LocalDateTime feedbackAt) { this.feedbackAt = feedbackAt; }
}
