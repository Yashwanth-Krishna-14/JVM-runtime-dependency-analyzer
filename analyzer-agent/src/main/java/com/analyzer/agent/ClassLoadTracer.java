package com.analyzer.agent;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.concurrent.atomic.AtomicInteger;

public class ClassLoadTracer implements ClassFileTransformer {

    private final com.analyzer.common.DependencyGraph graph;
    private final com.analyzer.common.util.ClassFilter classFilter;
    private final AtomicInteger filteredClassesCount = new AtomicInteger(0);
    private final AtomicInteger ignoredClassesCount = new AtomicInteger(0);

    public ClassLoadTracer(com.analyzer.common.DependencyGraph graph,
            com.analyzer.common.util.ClassFilter classFilter) {
        this.graph = graph;
        this.classFilter = classFilter;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) {

        if (className == null)
            return null;

        // Check if class is allowed by the filter
        if (!classFilter.isAllowed(className)) {
            ignoredClassesCount.incrementAndGet();
            return null;
        }

        filteredClassesCount.incrementAndGet();

        try {
            // System.out.println("[Analyzer] Inspecting " + className);
            org.objectweb.asm.ClassReader reader = new org.objectweb.asm.ClassReader(classfileBuffer);
            com.analyzer.core.ClassAnalyzer analyzer = new com.analyzer.core.ClassAnalyzer(graph, classFilter);

            // We can try to guess version/source from protection domain if available,
            // but for now let's just analyze dependencies.
            String source = (protectionDomain != null && protectionDomain.getCodeSource() != null)
                    ? protectionDomain.getCodeSource().getLocation().toString()
                    : "unknown";

            analyzer.setContext("runtime", source);
            reader.accept(analyzer, 0);

        } catch (Throwable e) {
            // Ignore errors during analysis to avoid crashing the app
            // e.printStackTrace();
        }

        // Return null means "no transformation", just introspection
        return null;
    }

    public int getFilteredClassesCount() {
        return filteredClassesCount.get();
    }

    public int getIgnoredClassesCount() {
        return ignoredClassesCount.get();
    }
}
