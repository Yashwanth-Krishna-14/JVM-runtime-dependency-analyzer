package com.analyzer.common.model;

import java.util.List;
import java.util.Map;

public class ArchitectureReport {
    private final Map<String, Integer> summary;
    private final List<ClassMetrics> classes;
    private final List<List<String>> cycles;

    public ArchitectureReport(int totalClasses, int totalDependencies, int totalCycles,
            List<ClassMetrics> classes, List<List<String>> cycles) {
        this.summary = Map.of(
                "totalClasses", totalClasses,
                "totalDependencies", totalDependencies,
                "totalCycles", totalCycles);
        this.classes = classes;
        this.cycles = cycles;
    }

    public Map<String, Integer> getSummary() {
        return summary;
    }

    public List<ClassMetrics> getClasses() {
        return classes;
    }

    public List<List<String>> getCycles() {
        return cycles;
    }
}
