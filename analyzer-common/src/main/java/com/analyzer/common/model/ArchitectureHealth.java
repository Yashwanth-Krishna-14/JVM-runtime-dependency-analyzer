package com.analyzer.common.model;

public class ArchitectureHealth {
    private final int score;
    private final String riskLevel;
    private final int totalCycles;
    private final int highlyCoupledClasses;
    private final int extremeInstabilityClasses;
    private final double dependencyDensity;

    public ArchitectureHealth(int score, String riskLevel, int totalCycles, int highlyCoupledClasses,
            int extremeInstabilityClasses, double dependencyDensity) {
        this.score = score;
        this.riskLevel = riskLevel;
        this.totalCycles = totalCycles;
        this.highlyCoupledClasses = highlyCoupledClasses;
        this.extremeInstabilityClasses = extremeInstabilityClasses;
        this.dependencyDensity = dependencyDensity;
    }

    public int getScore() {
        return score;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public int getTotalCycles() {
        return totalCycles;
    }

    public int getHighlyCoupledClasses() {
        return highlyCoupledClasses;
    }

    public int getExtremeInstabilityClasses() {
        return extremeInstabilityClasses;
    }

    public double getDependencyDensity() {
        return dependencyDensity;
    }
}
