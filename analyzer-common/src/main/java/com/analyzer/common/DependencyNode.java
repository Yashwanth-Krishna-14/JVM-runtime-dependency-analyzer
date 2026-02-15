package com.analyzer.common;

import java.util.HashSet;
import java.util.Set;
import java.util.Objects;

public class DependencyNode {
    private final String className;
    private final Set<String> dependencies;
    private final Set<String> usedBy;
    private String version;
    private String sourceJar;

    public DependencyNode(String className) {
        this.className = className;
        this.dependencies = new HashSet<>();
        this.usedBy = new HashSet<>();
    }

    public String getClassName() {
        return className;
    }

    public Set<String> getDependencies() {
        return dependencies;
    }

    public Set<String> getUsedBy() {
        return usedBy;
    }

    public void addDependency(String dependencyClass) {
        if (!dependencyClass.equals(this.className)) {
            dependencies.add(dependencyClass);
        }
    }

    public void addUsage(String userClass) {
        if (!userClass.equals(this.className)) {
            usedBy.add(userClass);
        }
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getSourceJar() {
        return sourceJar;
    }

    public void setSourceJar(String sourceJar) {
        this.sourceJar = sourceJar;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DependencyNode that = (DependencyNode) o;
        return Objects.equals(className, that.className);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className);
    }

    @Override
    public String toString() {
        return "DependencyNode{" +
                "className='" + className + '\'' +
                ", dependencies=" + dependencies.size() +
                '}';
    }
}
