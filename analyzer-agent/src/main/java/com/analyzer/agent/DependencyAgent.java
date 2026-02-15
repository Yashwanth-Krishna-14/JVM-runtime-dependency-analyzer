package com.analyzer.agent;

import com.analyzer.common.DependencyGraph;
import java.lang.instrument.Instrumentation;

public class DependencyAgent {

    private static final String OUTPUT_FILE = "dependency-graph.json";
    private static DependencyGraph graph;
    private static com.analyzer.common.util.ClassFilter classFilter;
    private static ClassLoadTracer tracer;

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("[DependencyAgent] Installing ClassFileTransformer (startup mode)...");
        initializeWithGraph(inst, agentArgs, false);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("[DependencyAgent] Agent attached dynamically...");
        initializeWithGraph(inst, agentArgs, true);
    }

    private static void initializeWithGraph(Instrumentation inst, String agentArgs, boolean retransformLoaded) {
        // Parse arguments: basePackage=com.example
        String basePackage = null;
        if (agentArgs != null) {
            String[] args = agentArgs.split(",");
            for (String arg : args) {
                if (arg.trim().startsWith("basePackage=")) {
                    basePackage = arg.trim().substring("basePackage=".length());
                }
            }
        }

        System.out
                .println("[DependencyAgent] Configuration: basePackage=" + (basePackage == null ? "ALL" : basePackage));

        graph = new DependencyGraph();
        classFilter = new com.analyzer.common.util.ClassFilter(basePackage);
        tracer = new ClassLoadTracer(graph, classFilter);
        inst.addTransformer(tracer, true);

        if (retransformLoaded) {
            try {
                System.out.println("[DependencyAgent] Retransforming loaded classes...");
                // Retransform only editable classes
                // In practice, retransforming EVERYTHING is risky and slow.
                // We typically only want to retransform classes we are interested in.
                // BUT, to get a graph, we need to inspect them.
                // For safety in this prototype, we might iterate all classes but careful with
                // JDK.
                // The transformer itself has guards.

                // For simplicity/safety in this prototype, we will attempt retransformation
                // but usually agents define a restrictive scope.
                Class<?>[] loadedClasses = inst.getAllLoadedClasses();
                for (Class<?> clazz : loadedClasses) {
                    if (inst.isModifiableClass(clazz) && classFilter.isAllowed(clazz.getName().replace('.', '/'))) {
                        try {
                            inst.retransformClasses(clazz);
                        } catch (Throwable t) {
                            // Ignore
                        }
                    }
                }
                System.out.println("[DependencyAgent] Retransformation complete.");
            } catch (Exception e) {
                e.printStackTrace();
            }
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

                // 4. Compute Health Score
                com.analyzer.core.analysis.ArchitectureHealthScorer healthScorer = new com.analyzer.core.analysis.ArchitectureHealthScorer();
                com.analyzer.common.model.ArchitectureHealth health = healthScorer.score(metrics, cycles, totalClasses,
                        totalDependencies);

                // 5. Construct Report
                com.analyzer.common.model.ArchitectureReport report = new com.analyzer.common.model.ArchitectureReport(
                        totalClasses, totalDependencies, totalCycles, metrics, cycles, health,
                        classFilter.getAnalysisScope(), tracer.getFilteredClassesCount(),
                        tracer.getIgnoredClassesCount());

                // 6. Serialize to JSON
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
