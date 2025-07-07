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
    private double height; // in cm
    private double weight; // in kg
    private String exerciseFrequency;
    private boolean familyHistory;
    private String diet;
    private String sugaryDrinkConsumption;
    private boolean highBloodPressure;
    private String stressLevel;

    // New lifestyle & daily tracking attributes
    private int dailySteps;
    private double waterIntake;
    private double sleepHours;
    private boolean smoking;
    private boolean alcohol;
    private String location;

    // User goals
    private int goalStepsPerDay;
    private double goalWaterPerDayL;
    private double goalSleepPerDayH;

    // Prediction output
    @Column(columnDefinition = "TEXT")
    private String predictionResult;
 
    private double bmi; 
    private double glucoseLevel; 

    // Utility method to calculate BMI
    public void calculateAndSetBMI() {
        if (height > 0 && weight > 0) {
            double heightInMeters = height / 100.0;
            this.bmi = Math.round((weight / (heightInMeters * heightInMeters)) * 100.0) / 100.0;
        }
    }

    public void setGlucoseLevel(double glucoseLevel) {
        this.glucoseLevel = glucoseLevel;
    }
}
 
	
