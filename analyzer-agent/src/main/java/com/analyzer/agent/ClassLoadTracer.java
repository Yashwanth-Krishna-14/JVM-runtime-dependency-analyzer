package com.analyzer.agent;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public class ClassLoadTracer implements ClassFileTransformer {

    private final com.analyzer.common.DependencyGraph graph;

    public ClassLoadTracer(com.analyzer.common.DependencyGraph graph) {
        this.graph = graph;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) {

        if (className == null)
            return null;

        // Skip JDK classes and our own agent classes to avoid infinite recursion/noise
        if (className.startsWith("java/") || className.startsWith("sun/") || className.startsWith("jdk/") ||
                className.startsWith("com/analyzer/agent/")) {
            return null;
        }

        try {
            // System.out.println("[Analyzer] Inspecting " + className);
            org.objectweb.asm.ClassReader reader = new org.objectweb.asm.ClassReader(classfileBuffer);
            com.analyzer.core.ClassAnalyzer analyzer = new com.analyzer.core.ClassAnalyzer(graph);

            // We can try to guess version/source from protection domain if available,
            // but for now let's just analyze dependencies.
            String source = (protectionDomain != null && protectionDomain.getCodeSource() != null)
                    ? protectionDomain.getCodeSource().getLocation().toString()
                    : "unknown";

            analyzer.setContext("runtime", source);
            reader.accept(analyzer, 0);

        } catch (Throwable t) {
            // Be very careful not to break the app if analysis fails
            t.printStackTrace();
        }

        // Return null to indicate no transformation (just inspection)
        return null;
    }
}
