package com.analyzer.agent;

import com.analyzer.common.DependencyGraph;
import java.lang.instrument.Instrumentation;

public class DependencyAgent {

    private static final String OUTPUT_FILE = "dependency-graph.json";
    private static DependencyGraph graph;

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("[DependencyAgent] Installing ClassFileTransformer (startup mode)...");
        initializeWithGraph(inst, false);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("[DependencyAgent] Agent attached dynamically...");
        initializeWithGraph(inst, true);
    }

    private static void initializeWithGraph(Instrumentation inst, boolean retransformLoaded) {
        graph = new DependencyGraph();

        ClassLoadTracer transformer = new ClassLoadTracer(graph);

        // Enable retransformation capability
        inst.addTransformer(transformer, true);

        // 🔥 Critical part for dynamic attach
        if (retransformLoaded) {
            System.out.println("[DependencyAgent] Retransforming already loaded classes...");

            for (Class<?> clazz : inst.getAllLoadedClasses()) {
                try {
                    if (inst.isModifiableClass(clazz)) {
                        inst.retransformClasses(clazz);
                    }
                } catch (Throwable ignored) {
                    // Some classes (like JVM internals) cannot be modified
                }
            }

            System.out.println("[DependencyAgent] Retransformation complete.");
        }

        // Shutdown hook to run analysis and dump report
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[DependencyAgent] Running architecture analysis...");

            try {
                // 1. Run Coupling Analysis
                com.analyzer.core.analysis.CouplingAnalyzer couplingAnalyzer = new com.analyzer.core.analysis.CouplingAnalyzer();
                java.util.List<com.analyzer.common.model.ClassMetrics> metrics = couplingAnalyzer.analyze(graph);

                // 2. Run Cycle Detection
                com.analyzer.core.analysis.CycleDetector cycleDetector = new com.analyzer.core.analysis.CycleDetector();
                java.util.List<java.util.List<String>> cycles = cycleDetector.detectCycles(graph);

                // 3. Compute Summary Stats
                int totalClasses = graph.getNodes().size();
                int totalDependencies = graph.calculateFanOut().values().stream().mapToInt(Integer::intValue).sum();
                int totalCycles = cycles.size();

                // 4. Construct Report
                com.analyzer.common.model.ArchitectureReport report = new com.analyzer.common.model.ArchitectureReport(
                        totalClasses, totalDependencies, totalCycles, metrics, cycles);

                // 5. Serialize to JSON
                System.out.println("[DependencyAgent] Dumping architecture report to " + OUTPUT_FILE + "...");
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                mapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);

                mapper.writeValue(new java.io.File(OUTPUT_FILE), report);

                System.out.println("[DependencyAgent] Analysis complete.");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
    }
}
