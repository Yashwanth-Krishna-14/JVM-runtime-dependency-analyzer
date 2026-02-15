package com.analyzer.cli;

import com.analyzer.common.DependencyGraph;
import com.analyzer.common.DependencyNode;
import com.analyzer.core.ArtifactScanner;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "analyzer", mixinStandardHelpOptions = true, version = "1.0", description = "JVM Runtime Dependency Analyzer", subcommands = {
        AnalyzeCommand.class, AttachCommand.class, CyclesCommand.class, ConflictsCommand.class })
public class Main implements Callable<Integer> {

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        CommandLine.usage(this, System.out);
        return 0;
    }
}

@Command(name = "analyze", description = "Analyze a JAR file or directory for dependencies")
class AnalyzeCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Input JAR file or directory to analyze")
    private File input;

    @Option(names = { "-o", "--output" }, description = "Output file for the report")
    private File output;

    @Option(names = { "-f", "--format" }, description = "Output format (text, json, dot)", defaultValue = "text")
    private String format;

    @Override
    public Integer call() throws Exception {
        if (!input.exists()) {
            System.err.println("Error: Input file " + input + " does not exist.");
            return 1;
        }

        System.out.println("Analyzing " + input.getAbsolutePath() + "...");

        DependencyGraph graph = new DependencyGraph();
        ArtifactScanner scanner = new ArtifactScanner(graph);
        scanner.scanPath(input.toPath());

        System.out.println("Analysis complete. Found " + graph.getNodes().size() + " classes.");

        if (output != null) {
            try (java.io.PrintStream ps = new java.io.PrintStream(output)) {
                System.setOut(ps);
                runOutput(format, graph);
            } catch (java.io.FileNotFoundException e) {
                System.err.println("Error: Could not write to output file " + output);
                return 1;
            } finally {
                System.setOut(new java.io.PrintStream(new java.io.FileOutputStream(java.io.FileDescriptor.out)));
            }
        } else {
            runOutput(format, graph);
        }

        return 0;
    }

    private void runOutput(String format, DependencyGraph graph) {
        switch (format.toLowerCase()) {
            case "json":
                System.out.println("JSON output not yet implemented.");
                break;
            case "dot":
                printDot(graph);
                break;
            case "text":
            default:
                printText(graph);
                break;
        }
    }

    private void printText(DependencyGraph graph) {
        for (DependencyNode node : graph.getNodes()) {
            if (!node.getDependencies().isEmpty()) {
                System.out.println(node.getClassName());
                for (String dep : node.getDependencies()) {
                    System.out.println("  -> " + dep);
                }
            }
        }
    }

    private void printDot(DependencyGraph graph) {
        System.out.println("digraph dependencies {");
        for (DependencyNode node : graph.getNodes()) {
            for (String dep : node.getDependencies()) {
                System.out.printf("  \"%s\" -> \"%s\";%n", node.getClassName(), dep);
            }
        }
        System.out.println("}");
    }
}

@Command(name = "attach", description = "Attach to a running JVM process")
class AttachCommand implements Callable<Integer> {

    @Option(names = { "-p", "--pid" }, required = true, description = "Process ID of the target JVM")
    private String pid;

    @Option(names = { "-a", "--agent" }, required = true, description = "Path to the analyzer-agent.jar")
    private File agentJar;

    @Override
    public Integer call() throws Exception {
        if (!agentJar.exists()) {
            System.err.println("Error: Agent JAR " + agentJar + " does not exist.");
            return 1;
        }

        System.out.println("Attaching to process " + pid + " with agent " + agentJar.getAbsolutePath());

        try {
            Class<?> vmClass = Class.forName("com.sun.tools.attach.VirtualMachine");
            java.lang.reflect.Method attachMethod = vmClass.getMethod("attach", String.class);
            Object vm = attachMethod.invoke(null, pid);

            java.lang.reflect.Method loadAgentMethod = vmClass.getMethod("loadAgent", String.class);
            loadAgentMethod.invoke(vm, agentJar.getAbsolutePath());

            java.lang.reflect.Method detachMethod = vmClass.getMethod("detach");
            detachMethod.invoke(vm);

            System.out.println("Agent attached successfully.");
        } catch (ClassNotFoundException e) {
            System.err.println("Error: com.sun.tools.attach.VirtualMachine not found. Are you running on a JDK?");
            return 1;
        } catch (Exception e) {
            System.err.println("Error attaching to process: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }

        return 0;
    }
}

@Command(name = "cycles", description = "Detect circular dependencies")
class CyclesCommand implements Callable<Integer> {
    @Parameters(index = "0", description = "Input JAR file or directory")
    private File input;

    @Override
    public Integer call() throws Exception {
        DependencyGraph graph = new DependencyGraph();
        new ArtifactScanner(graph).scanPath(input.toPath());

        System.out.println("Scanning for cycles...");
        List<List<String>> cycles = graph.detectCycles();
        if (cycles.isEmpty()) {
            System.out.println("No circular dependencies found.");
        } else {
            System.out.println("Found " + cycles.size() + " cycles:");
            for (List<String> cycle : cycles) {
                System.out.println(" - " + String.join(" -> ", cycle));
            }
        }
        return 0;
    }
}

@Command(name = "conflicts", description = "Detect version conflicts")
class ConflictsCommand implements Callable<Integer> {
    @Parameters(index = "0", description = "Input JAR file or directory")
    private File input;

    @Override
    public Integer call() throws Exception {
        DependencyGraph graph = new DependencyGraph();
        new ArtifactScanner(graph).scanPath(input.toPath());

        System.out.println("Scanning for conflicts...");
        List<String> conflicts = graph.detectConflicts();
        if (conflicts.isEmpty()) {
            System.out.println("No version conflicts detected.");
        } else {
            System.out.println("Found potential conflicts:");
            for (String conflict : conflicts) {
                System.out.println(" - " + conflict);
            }
        }
        return 0;
    }
}
