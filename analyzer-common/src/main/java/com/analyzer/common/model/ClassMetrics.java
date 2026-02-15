package com.analyzer.common.model;

public class ClassMetrics {
    private final String className;
    private final int efferentCoupling;
    private final int afferentCoupling;
    private final double instability;

    public ClassMetrics(String className, int efferentCoupling, int afferentCoupling, double instability) {
        this.className = className;
        this.efferentCoupling = efferentCoupling;
        this.afferentCoupling = afferentCoupling;
        this.instability = instability;
    }

    public String getClassName() {
        return className;
    }

    public int getEfferentCoupling() {
        return efferentCoupling;
    }

    public int getAfferentCoupling() {
        return afferentCoupling;
    }

    public double getInstability() {
        return instability;
    }
}
