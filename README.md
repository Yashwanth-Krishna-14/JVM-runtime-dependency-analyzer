# JVM Dependency Analyzer

Runtime & Static Architecture Analysis for Java Applications

A modular tool that analyzes Java bytecode to build a directed dependency graph of class relationships, detect circular dependencies, compute coupling metrics, and generate an architecture health score.

---

## Why This Project

As Java applications grow:

- Circular dependencies go unnoticed  
- Coupling increases silently  
- Architectural decay becomes harder to detect  
- Runtime behavior differs from static assumptions  

Most tools focus on performance and logging.  
This tool focuses on **structural integrity at the class level**.

---

## What It Does

### Static Analysis
- Scans JAR files or directories
- Parses raw `.class` files using ASM
- Extracts dependencies without loading classes
- Generates a structured JSON report

### Dynamic Analysis (Java Agent)
- Attaches to a running JVM
- Intercepts class loading using `ClassFileTransformer`
- Builds the dependency graph in real time
- Generates report on application shutdown

---

## Core Concepts

### Dependency Graph

- Directed graph: `ClassA → ClassB`
- Backed by `Map<String, DependencyNode>`
- Thread-safe updates
- Supports incremental runtime analysis

All metrics are derived from this graph.

---

### Bytecode-Level Analysis

Uses ASM (`org.objectweb.asm`) with a custom `ClassVisitor` to extract:

- Superclasses and interfaces
- Field types
- Method parameters and return types
- Generic signatures
- Local variable descriptors

No reflection.  
No class loading.  
Pure bytecode inspection.

---

### Coupling Metrics

Implements Robert C. Martin’s instability metric:

- **Ce (Efferent Coupling)** – outgoing dependencies  
- **Ca (Afferent Coupling)** – incoming dependencies  

Formula: I = Ce / (Ce + Ca)

This measures architectural stability.

---

### Cycle Detection

Uses Tarjan’s Strongly Connected Components algorithm.

Time complexity: O(V + E)


### Architecture Health Score

A heuristic engine evaluates:

- Number of cycles  
- Coupling intensity  
- Graph density  

Example output:

```json
{
  "summary": {
    "totalClasses": 412,
    "totalDependencies": 1432,
    "totalCycles": 5
  },
  "health": {
    "score": 78,
    "riskLevel": "GOOD"
  }
}


## Project Structure

jvm-dependency-analyzer/
├── analyzer-common   # Graph models & DTOs
├── analyzer-core     # Bytecode analysis & algorithms
├── analyzer-agent    # Java Agent (runtime instrumentation)
└── analyzer-cli      # CLI interface

## Tech Stack

- **Java 11+** — Core language used to build the analysis engine and agent.

- **ASM** — Bytecode manipulation and inspection library used to parse `.class` files without loading them.

- **Jackson** — JSON serialization library used to generate structured architecture reports.

- **Picocli** — Lightweight CLI framework used to build the command-line interface.

- **Maven** — Build and dependency management tool with multi-module project support.

- **Java Instrumentation API** — JVM API used to attach a Java Agent and intercept class loading at runtime.