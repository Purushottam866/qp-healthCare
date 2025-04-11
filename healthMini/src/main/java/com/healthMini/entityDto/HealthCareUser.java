package com.healthMini.entityDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.healthMini.enums.SubscriptionPlan;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Component
@Entity
@Table(name = "healthcare_user")
@Data
public class HealthCareUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int userId;

    private String fullName;
    
    private String email;

    private String phoneNumber;

    private String password;

    private LocalDate dateOfBirth;

    private String gender;

    private String role;

    private int otp;

    private LocalDateTime otpGeneratedAt;

    private boolean isEmailVerified;

    private LocalDateTime createdAt;
    
    @Column(name = "last_prompt_time")
    private LocalDateTime lastPromptTime;

    @Enumerated(EnumType.STRING) // Store enum as string in DB
    @Column(name = "subscription_plan", length = 20, nullable = false)
    private SubscriptionPlan subscriptionPlan;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatSession> sessions;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HealthData> healthDataList = new ArrayList<>();


    public boolean isOtpExpired() {
        if (this.otpGeneratedAt == null) {
            return true;
        }
        return LocalDateTime.now().isAfter(this.otpGeneratedAt.plusMinutes(30));
    }

    public void resetOtp() {
        this.otp = 0;
        this.otpGeneratedAt = null;
    }
}

