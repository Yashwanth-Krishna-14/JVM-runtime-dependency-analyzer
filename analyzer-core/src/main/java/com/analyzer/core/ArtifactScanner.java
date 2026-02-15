package com.analyzer.core;

import com.analyzer.common.DependencyGraph;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ArtifactScanner {
    private final DependencyGraph graph;

    public ArtifactScanner(DependencyGraph graph) {
        this.graph = graph;
    }

    public void scanPath(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            scanDirectory(path);
        } else if (path.toString().endsWith(".jar")) {
            scanJar(path);
        }
    }

    private void scanDirectory(Path dir) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".class")) {
                    try (InputStream is = Files.newInputStream(file)) {
                        analyzeClass(is);
                    }
                } else if (file.toString().endsWith(".jar")) {
                    scanJar(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void scanJar(Path jarPath) throws IOException {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    try (InputStream is = jarFile.getInputStream(entry)) {
                        // Basic version extraction from JAR name
                        String jarName = jarPath.getFileName().toString();
                        String version = extractVersion(jarName);

                        // We need to pass this version info to the analyzer or graph
                        analyzeClass(is, version, jarName);
                    }
                }
            }
        }
    }

    private void analyzeClass(InputStream is) throws IOException {
        analyzeClass(is, null, null);
    }

    private void analyzeClass(InputStream is, String version, String source) throws IOException {
        try {
            ClassReader reader = new ClassReader(is);
            ClassAnalyzer analyzer = new ClassAnalyzer(graph);
            // We need a way to set version/source on the node created/accessed by the
            // visitor
            // For now, let's pre-register the node if we know the name,
            // OR we can make ClassAnalyzer accept these as context.
            // A better way: ClassAnalyzer sets the graph, we can augment the node AFTER
            // analysis
            // if we know the class name, but ClassReader visits the class name first.
            // Let's pass context to ClassAnalyzer.
            analyzer.setContext(version, source);
            reader.accept(analyzer, 0);
        } catch (Exception e) {
            System.err.println("Failed to analyze class: " + e.getMessage());
        }
    }

    private String extractVersion(String jarName) {
        // Simple regex or string manipulation to find version in jar name (e.g.,
        // app-1.0.0.jar)
        // This is heuristic-based.
        if (jarName.endsWith(".jar")) {
            jarName = jarName.substring(0, jarName.length() - 4);
        }
        int dashIndex = jarName.lastIndexOf('-');
        if (dashIndex != -1 && dashIndex < jarName.length() - 1) {
            char firstChar = jarName.charAt(dashIndex + 1);
            if (Character.isDigit(firstChar)) {
                return jarName.substring(dashIndex + 1);
            }
        }
        return "unknown";
    }
}
