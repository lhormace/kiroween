package com.health.chat.model;

public class DailyNutrition {
    private double totalCalories;
    private PFCBalance pfcBalance;

    public DailyNutrition() {
    }

    public DailyNutrition(double totalCalories, PFCBalance pfcBalance) {
        this.totalCalories = totalCalories;
        this.pfcBalance = pfcBalance;
    }

    public double getTotalCalories() {
        return totalCalories;
    }

    public void setTotalCalories(double totalCalories) {
        this.totalCalories = totalCalories;
    }

    public PFCBalance getPfcBalance() {
        return pfcBalance;
    }

    public void setPfcBalance(PFCBalance pfcBalance) {
        this.pfcBalance = pfcBalance;
    }
}
