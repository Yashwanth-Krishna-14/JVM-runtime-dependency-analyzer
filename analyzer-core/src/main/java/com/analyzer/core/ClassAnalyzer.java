package com.analyzer.core;

import com.analyzer.common.DependencyGraph;
import org.objectweb.asm.*;

import com.analyzer.common.DependencyNode;

public class ClassAnalyzer extends ClassVisitor {
    private final DependencyGraph graph;
    private String currentClassName;
    private String version;
    private String source;

    public ClassAnalyzer(DependencyGraph graph) {
        super(Opcodes.ASM9);
        this.graph = graph;
    }

    public void setContext(String version, String source) {
        this.version = version;
        this.source = source;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.currentClassName = name.replace('/', '.');
        DependencyNode node = graph.getOrCreateNode(currentClassName);
        if (this.version != null)
            node.setVersion(this.version);
        if (this.source != null)
            node.setSourceJar(this.source);

        if (superName != null && !superName.equals("java/lang/Object")) {
            addDependency(superName);
        }

        if (interfaces != null) {
            for (String iface : interfaces) {
                addDependency(iface);
            }
        }
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        addDescriptorDependency(descriptor);
        if (signature != null) {
            // TODO: Analyze generics in signature
        }
        return new FieldVisitor(Opcodes.ASM9) {
            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                addDescriptorDependency(descriptor);
                return super.visitAnnotation(descriptor, visible);
            }
        };
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
            String[] exceptions) {
        addMethodDescriptorDependency(descriptor);

        if (exceptions != null) {
            for (String ex : exceptions) {
                addDependency(ex);
            }
        }

        return new MethodVisitor(Opcodes.ASM9) {
            @Override
            public void visitTypeInsn(int opcode, String type) {
                addDependency(type);
            }

            @Override
            public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                addDependency(owner);
                addDescriptorDependency(descriptor);
            }

            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                addDependency(owner);
                addMethodDescriptorDependency(descriptor);
            }

            @Override
            public void visitLdcInsn(Object value) {
                if (value instanceof Type) {
                    addTypeDependency((Type) value);
                }
            }

            @Override
            public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
                addDescriptorDependency(descriptor);
            }

            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                addDescriptorDependency(descriptor);
                return super.visitAnnotation(descriptor, visible);
            }
        };
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        addDescriptorDependency(descriptor);
        return super.visitAnnotation(descriptor, visible);
    }

    private void addDependency(String internalName) {
        if (internalName == null)
            return;
        // ASM uses internal names like java/lang/String
        // Arrays are like [Ljava/lang/String;
        Type type = Type.getObjectType(internalName);
        addTypeDependency(type);
    }

    private void addTypeDependency(Type type) {
        if (type.getSort() == Type.ARRAY) {
            type = type.getElementType();
        }
        if (type.getSort() == Type.OBJECT) {
            String className = type.getClassName();
            if (!className.equals(currentClassName)) {
                graph.addDependency(currentClassName, className);
            }
        }
    }

    private void addDescriptorDependency(String descriptor) {
        Type type = Type.getType(descriptor);
        addTypeDependency(type);
    }

    private void addMethodDescriptorDependency(String descriptor) {
        Type methodType = Type.getMethodType(descriptor);
        addTypeDependency(methodType.getReturnType());
        for (Type argType : methodType.getArgumentTypes()) {
            addTypeDependency(argType);
        }
    }
}
