package com.analyzer.common.model;

import java.util.List;
import java.util.Map;
import com.analyzer.common.model.ArchitectureHealth;

public class ArchitectureReport {
    private final Map<String, Object> summary;
    private final ArchitectureHealth health;
    private final List<ClassMetrics> classes;
    private final List<List<String>> cycles;

    public ArchitectureReport(int totalClasses, int totalDependencies, int totalCycles,
            List<ClassMetrics> classes, List<List<String>> cycles,
            ArchitectureHealth health,
            String analysisScope, int filteredClassesCount, int ignoredClassesCount) {
        this.summary = new java.util.LinkedHashMap<>();
        this.summary.put("analysisScope", analysisScope);
        this.summary.put("totalClasses", totalClasses);
        this.summary.put("filteredClassesCount", filteredClassesCount);
        this.summary.put("ignoredClassesCount", ignoredClassesCount);
        this.summary.put("totalDependencies", totalDependencies);
        this.summary.put("totalCycles", totalCycles);

        this.health = health;
        this.classes = classes;
        this.cycles = cycles;
    }

    public Map<String, Object> getSummary() {
        return summary;
    }

    public ArchitectureHealth getHealth() {
        return health;
    }

    public List<ClassMetrics> getClasses() {
        return classes;
    }

    public List<List<String>> getCycles() {
        return cycles;
    }
}
