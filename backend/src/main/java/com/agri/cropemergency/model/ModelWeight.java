package com.agri.cropemergency.model;

import jakarta.persistence.*;

/** Learned sensitivity per threat type - adjusted by farmer feedback (continuous learning). */
@Entity
@Table(name = "model_weights")
public class ModelWeight {
    @Id
    private String threat;

    private double sensitivity = 1.0;
    private int confirmedCount = 0;
    private int falseAlarmCount = 0;
    private int missedCount = 0;

    public ModelWeight() {}
    public ModelWeight(String threat) { this.threat = threat; }

    public String getThreat() { return threat; }
    public void setThreat(String threat) { this.threat = threat; }
    public double getSensitivity() { return sensitivity; }
    public void setSensitivity(double sensitivity) { this.sensitivity = sensitivity; }
    public int getConfirmedCount() { return confirmedCount; }
    public void setConfirmedCount(int confirmedCount) { this.confirmedCount = confirmedCount; }
    public int getFalseAlarmCount() { return falseAlarmCount; }
    public void setFalseAlarmCount(int falseAlarmCount) { this.falseAlarmCount = falseAlarmCount; }
    public int getMissedCount() { return missedCount; }
    public void setMissedCount(int missedCount) { this.missedCount = missedCount; }
}
