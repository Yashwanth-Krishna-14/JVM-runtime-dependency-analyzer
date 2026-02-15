package com.analyzer.core.analysis;

import com.analyzer.common.DependencyGraph;
import com.analyzer.common.DependencyNode;
import com.analyzer.common.model.ClassMetrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CouplingAnalyzer {

    public List<ClassMetrics> analyze(DependencyGraph graph) {
        Map<String, Integer> fanOut = graph.calculateFanOut();
        Map<String, Integer> fanIn = graph.calculateFanIn();
        List<ClassMetrics> metricsList = new ArrayList<>();

        for (DependencyNode node : graph.getNodes()) {
            String className = node.getClassName();
            int ce = fanOut.getOrDefault(className, 0);
            int ca = fanIn.getOrDefault(className, 0);
            double instability = 0.0;

            if (ca + ce > 0) {
                instability = (double) ce / (ca + ce);
            }

            metricsList.add(new ClassMetrics(className, ce, ca, instability));
        }

        return metricsList;
    }
}
