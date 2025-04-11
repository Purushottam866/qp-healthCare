package com.healthMini.enums;


public enum SubscriptionPlan {
    FREE(5),       // 5 inputs per day
    BASIC(10),     // 10 inputs per day
    PREMIUM(20),   // 20 inputs per day
    ADMIN(Integer.MAX_VALUE); // Unlimited access for admin users

    private final int dailyLimit;

    SubscriptionPlan(int dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public int getDailyLimit() {
        return dailyLimit;
    }
}