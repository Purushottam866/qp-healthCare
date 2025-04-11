package com.healthMini.entityDto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "health_data")
@Data
public class HealthData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer healthDataId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private HealthCareUser user;

    private int age;
    private double height;
    private double weight;
    private String exerciseFrequency;
    private boolean familyHistory;
    private String diet;
    private String sugaryDrinkConsumption;
    private boolean highBloodPressure;
    private String stressLevel;

    @Column(columnDefinition = "TEXT")
    private String predictionResult;
    private double bmi;
    private double glucoseLevel;

    // Getters and setters

    public void calculateAndSetBMI() {
        if (height > 0 && weight > 0) {
            double heightInMeters = height / 100.0; // Convert cm to meters
            this.bmi = Math.round((weight / (heightInMeters * heightInMeters)) * 100.0) / 100.0;
        }
    }

    public void setGlucoseLevel(double glucoseLevel) {
        this.glucoseLevel = glucoseLevel;
    }
}
	
