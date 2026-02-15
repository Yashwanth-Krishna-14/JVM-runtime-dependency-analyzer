package com.analyzer.core.analysis;

import com.analyzer.common.model.ArchitectureHealth;
import com.analyzer.common.model.ClassMetrics;

import java.util.List;

public class ArchitectureHealthScorer {

    private static final int BASE_SCORE = 100;
    private static final int PENALTY_PER_CYCLE = 10;
    private static final int PENALTY_PER_HIGH_COUPLING = 5;
    private static final int PENALTY_PER_EXTREME_INSTABILITY = 3;
    private static final int HIGH_COUPLING_THRESHOLD = 15;
    private static final double INSTABILITY_LOW = 0.1;
    private static final double INSTABILITY_HIGH = 0.9;
    private static final double DENSITY_THRESHOLD = 20.0;

    public ArchitectureHealth score(List<ClassMetrics> classes, List<List<String>> cycles, int totalClasses,
            int totalDependencies) {
        int score = BASE_SCORE;
        int highlyCoupledClasses = 0;
        int extremeInstabilityClasses = 0;

        // A) Cycle Penalty
        score -= (cycles.size() * PENALTY_PER_CYCLE);

        for (ClassMetrics m : classes) {
            // B) High Coupling Penalty
            if (m.getEfferentCoupling() > HIGH_COUPLING_THRESHOLD) {
                highlyCoupledClasses++;
                score -= PENALTY_PER_HIGH_COUPLING;
            }

            // C) Instability Imbalance
            if (m.getInstability() < INSTABILITY_LOW || m.getInstability() > INSTABILITY_HIGH) {
                extremeInstabilityClasses++;
                score -= PENALTY_PER_EXTREME_INSTABILITY;
            }
        }

        // D) Dependency Density Penalty
        double density = totalClasses > 0 ? (double) totalDependencies / totalClasses : 0;
        if (density > DENSITY_THRESHOLD) {
            score -= (int) (density - DENSITY_THRESHOLD);
        }

        // Clamp Score
        score = Math.max(0, Math.min(100, score));

        // Determine Risk Level
        String riskLevel = getRiskLevel(score);

        return new ArchitectureHealth(score, riskLevel, cycles.size(), highlyCoupledClasses, extremeInstabilityClasses,
                density);
    }

    private String getRiskLevel(int score) {
        if (score >= 90)
            return "EXCELLENT";
        if (score >= 75)
            return "HEALTHY";
        if (score >= 60)
            return "MODERATE";
        if (score >= 40)
            return "HIGH_RISK";
        return "CRITICAL";
    }
}
