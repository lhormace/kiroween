package com.health.chat.model;

public class PFCBalance {
    private double proteinRatio;
    private double fatRatio;
    private double carbohydrateRatio;

    public PFCBalance() {
    }

    public PFCBalance(double proteinRatio, double fatRatio, double carbohydrateRatio) {
        this.proteinRatio = proteinRatio;
        this.fatRatio = fatRatio;
        this.carbohydrateRatio = carbohydrateRatio;
    }

    public double getProteinRatio() {
        return proteinRatio;
    }

    public void setProteinRatio(double proteinRatio) {
        this.proteinRatio = proteinRatio;
    }

    public double getFatRatio() {
        return fatRatio;
    }

    public void setFatRatio(double fatRatio) {
        this.fatRatio = fatRatio;
    }

    public double getCarbohydrateRatio() {
        return carbohydrateRatio;
    }

    public void setCarbohydrateRatio(double carbohydrateRatio) {
        this.carbohydrateRatio = carbohydrateRatio;
    }
}
