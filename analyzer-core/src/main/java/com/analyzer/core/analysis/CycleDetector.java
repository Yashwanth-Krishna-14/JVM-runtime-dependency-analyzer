package com.analyzer.core.analysis;

import com.analyzer.common.DependencyGraph;
import com.analyzer.common.DependencyNode;

import java.util.*;

public class CycleDetector {

    public List<List<String>> detectCycles(DependencyGraph graph) {
        List<List<String>> cycles = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();

        // Ensure consistent ordering for deterministic results
        List<String> sortedNodes = new ArrayList<>();
        for (DependencyNode node : graph.getNodes()) {
            sortedNodes.add(node.getClassName());
        }
        Collections.sort(sortedNodes);

        for (String nodeName : sortedNodes) {
            detectCycleUtil(nodeName, graph, visited, recursionStack, new ArrayList<>(), cycles);
        }
        return cycles;
    }

    private void detectCycleUtil(String current, DependencyGraph graph, Set<String> visited, Set<String> recursionStack,
            List<String> path, List<List<String>> cycles) {

        if (recursionStack.contains(current)) {
            // Cycle detected
            List<String> cyclePath = new ArrayList<>(path);
            int startIndex = cyclePath.indexOf(current);
            if (startIndex != -1) {
                List<String> cycle = new ArrayList<>(cyclePath.subList(startIndex, cyclePath.size()));
                cycle.add(current); // Close the loop
                cycles.add(cycle);
            }
            return;
        }

        if (visited.contains(current)) {
            return;
        }

        visited.add(current);
        recursionStack.add(current);
        path.add(current);

        Optional<DependencyNode> nodeOpt = graph.getNode(current);
        if (nodeOpt.isPresent()) {
            List<String> dependencies = new ArrayList<>(nodeOpt.get().getDependencies());
            Collections.sort(dependencies); // Deterministic order

            for (String dep : dependencies) {
                detectCycleUtil(dep, graph, visited, recursionStack, path, cycles);
            }
        }

        recursionStack.remove(current);
        path.remove(path.size() - 1);
    }
}
