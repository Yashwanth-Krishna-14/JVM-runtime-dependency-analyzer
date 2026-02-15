package com.analyzer.common.util;

public class ClassFilter {
    private final String basePackage;
    private final String internalBasePackage;

    public ClassFilter(String basePackage) {
        this.basePackage = basePackage;
        this.internalBasePackage = basePackage == null ? null : basePackage.replace('.', '/');
    }

    public boolean isAllowed(String className) {
        if (className == null)
            return false;

        // Always include if no base package is defined (default behavior)
        if (internalBasePackage == null || internalBasePackage.isEmpty()) {
            return !isExcludedByDefault(className);
        }

        // Must start with base package
        if (!className.startsWith(internalBasePackage)) {
            return false;
        }

        return !isExcludedByDefault(className);
    }

    public boolean isDependencyAllowed(String dependencyName) {
        // Same logic for dependencies - we only want to track edges within the
        // application scope
        return isAllowed(dependencyName);
    }

    private boolean isExcludedByDefault(String className) {
        // Exclude Lambda generated classes
        if (className.contains("$$Lambda$"))
            return true;

        // Exclude CGLIB proxies
        if (className.contains("$$EnhancerBySpringCGLIB$$"))
            return true;

        // Exclude JDK dynamic proxies
        if (className.startsWith("jdk/proxy/"))
            return true;
        if (className.startsWith("com/sun/proxy/"))
            return true;

        // Exclude Analysis Agent itself to avoid self-inspection loops if packages
        // overlap
        if (className.startsWith("com/analyzer/"))
            return true;

        return false;
    }

    public String getAnalysisScope() {
        return basePackage == null ? "ALL" : basePackage;
    }
}
