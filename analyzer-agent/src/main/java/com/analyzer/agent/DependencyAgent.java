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

        // Shutdown hook to dump graph
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[DependencyAgent] Dumping dependency graph to " + OUTPUT_FILE + "...");

            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                mapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);

                // We want to serialize the nodes. graph.getNodes() returns a
                // Set<DependencyNode>
                // Jackson should handle this POJO serialization automatically.
                mapper.writeValue(new java.io.File(OUTPUT_FILE), graph.getNodes());

                System.out.println("[DependencyAgent] Dump complete.");

            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }));
    }
}
