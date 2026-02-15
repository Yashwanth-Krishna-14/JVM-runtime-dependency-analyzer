package com.analyzer.common;

import java.util.*;
import java.util.stream.Collectors;

public class DependencyGraph {
    private final Map<String, DependencyNode> nodes = new HashMap<>();

    public DependencyNode getOrCreateNode(String className) {
        return nodes.computeIfAbsent(className, DependencyNode::new);
    }

    public void addDependency(String from, String to) {
        DependencyNode fromNode = getOrCreateNode(from);
        DependencyNode toNode = getOrCreateNode(to);

        fromNode.addDependency(to);
        toNode.addUsage(from);
    }

    public Set<DependencyNode> getNodes() {
        return new HashSet<>(nodes.values());
    }

    public Optional<DependencyNode> getNode(String className) {
        return Optional.ofNullable(nodes.get(className));
    }

    public List<List<String>> detectCycles() {
        List<List<String>> cycles = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();

        for (String nodeName : nodes.keySet()) {
            if (detectCycleUtil(nodeName, visited, recursionStack, new ArrayList<>(), cycles)) {
                // Cycle detected starting from this node
            }
        }
        return cycles;
    }

    private boolean detectCycleUtil(String current, Set<String> visited, Set<String> recursionStack,
            List<String> path, List<List<String>> cycles) {
        if (recursionStack.contains(current)) {
            // Found a cycle
            List<String> cyclePath = new ArrayList<>(path);
            // Trim the path to start from the first occurrence of 'current'
            int startIndex = cyclePath.indexOf(current);
            if (startIndex != -1) {
                cycles.add(new ArrayList<>(cyclePath.subList(startIndex, cyclePath.size())));
            } else {
                cyclePath.add(current); // Close the loop
                cycles.add(cyclePath);
            }
            return true;
        }

        if (visited.contains(current)) {
            return false;
        }

        visited.add(current);
        recursionStack.add(current);
        path.add(current);

        DependencyNode node = nodes.get(current);
        if (node != null) {
            for (String dep : node.getDependencies()) {
                if (detectCycleUtil(dep, visited, recursionStack, path, cycles)) {
                    // Don't return true immediately if we want to find all cycles,
                    // but for basic detection, we can continue
                }
            }
        }

        recursionStack.remove(current);
        path.remove(path.size() - 1);
        return false;
    }

    public Map<String, Integer> calculateFanOut() {
        return nodes.values().stream()
                .collect(Collectors.toMap(DependencyNode::getClassName, n -> n.getDependencies().size()));
    }

    public Map<String, Integer> calculateFanIn() {
        return nodes.values().stream()
                .collect(Collectors.toMap(DependencyNode::getClassName, n -> n.getUsedBy().size()));
    }

    /**
     * Detects potential conflicts where disjoint classes might belong to the same
     * library but different versions.
     * This is a heuristic based on package names and source JARs.
     */
    public List<String> detectConflicts() {
        List<String> conflicts = new ArrayList<>();
        Map<String, Set<String>> packageToVersions = new HashMap<>();

        for (DependencyNode node : nodes.values()) {
            if (node.getSourceJar() != null && node.getVersion() != null) {
                String packageName = getPackageName(node.getClassName());
                // Only track conflicts for roughly the same library (same package root +
                // distinct versions)
                // This is very basic.
                packageToVersions.computeIfAbsent(packageName, k -> new HashSet<>())
                        .add(node.getVersion() + " (" + node.getSourceJar() + ")");
            }
        }

        for (Map.Entry<String, Set<String>> entry : packageToVersions.entrySet()) {
            if (entry.getValue().size() > 1) {
                conflicts.add("Package " + entry.getKey() + " found in multiple versions/jars: " + entry.getValue());
            }
        }
        return conflicts;
    }

    private String getPackageName(String className) {
        int lastDot = className.lastIndexOf('.');
        return lastDot == -1 ? "" : className.substring(0, lastDot);
    }
}
