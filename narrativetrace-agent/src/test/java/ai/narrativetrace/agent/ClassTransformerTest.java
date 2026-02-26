package ai.narrativetrace.agent;

import ai.narrativetrace.core.context.ThreadLocalNarrativeContext;
import ai.narrativetrace.core.event.ParameterCapture;
import ai.narrativetrace.core.event.TraceOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import static org.assertj.core.api.Assertions.assertThat;

class ClassTransformerTest {

    private ThreadLocalNarrativeContext context;

    @BeforeEach
    void setUp() {
        context = new ThreadLocalNarrativeContext();
        context.reset();
        AgentRuntime.setContext(context);
    }

    @Test
    void transformedClassProducesCorrectTraceForSimpleMethod() throws Exception {
        // Get the original bytecode of the sample class
        var originalBytes = getClass().getClassLoader()
                .getResourceAsStream("ai/narrativetrace/agent/sample/Calculator.class")
                .readAllBytes();

        // Transform it
        var transformed = ClassTransformer.transform(originalBytes,
                "ai/narrativetrace/agent/sample/Calculator");

        // Load transformed class
        var loader = new ByteArrayClassLoader(getClass().getClassLoader(), transformed,
                "ai.narrativetrace.agent.sample.Calculator");
        var clazz = loader.loadClass("ai.narrativetrace.agent.sample.Calculator");
        var instance = clazz.getDeclaredConstructor().newInstance();
        var method = clazz.getMethod("add", int.class, int.class);

        var result = method.invoke(instance, 3, 4);

        assertThat(result).isEqualTo(7);

        var tree = context.captureTrace();
        assertThat(tree.roots()).hasSize(1);
        var root = tree.roots().get(0);
        assertThat(root.signature().className()).isEqualTo("ai.narrativetrace.agent.sample.Calculator");
        assertThat(root.signature().methodName()).isEqualTo("add");
        assertThat(root.outcome()).isInstanceOf(TraceOutcome.Returned.class);
        assertThat(((TraceOutcome.Returned) root.outcome()).renderedValue()).isEqualTo("7");
    }

    @Test
    void transformedClassCapturesExceptionInTrace() throws Exception {
        var originalBytes = getClass().getClassLoader()
                .getResourceAsStream("ai/narrativetrace/agent/sample/Calculator.class")
                .readAllBytes();

        var transformed = ClassTransformer.transform(originalBytes,
                "ai/narrativetrace/agent/sample/Calculator");

        var loader = new ByteArrayClassLoader(getClass().getClassLoader(), transformed,
                "ai.narrativetrace.agent.sample.Calculator");
        var clazz = loader.loadClass("ai.narrativetrace.agent.sample.Calculator");
        var instance = clazz.getDeclaredConstructor().newInstance();
        var method = clazz.getMethod("divide", int.class, int.class);

        try {
            method.invoke(instance, 10, 0);
        } catch (Exception e) {
            // expected
        }

        var tree = context.captureTrace();
        assertThat(tree.roots()).hasSize(1);
        var root = tree.roots().get(0);
        assertThat(root.signature().methodName()).isEqualTo("divide");
        assertThat(root.outcome()).isInstanceOf(TraceOutcome.Threw.class);
    }

    @Test
    void transformedClassCapturesParameterNamesAndValues() throws Exception {
        var originalBytes = getClass().getClassLoader()
                .getResourceAsStream("ai/narrativetrace/agent/sample/Calculator.class")
                .readAllBytes();

        var transformed = ClassTransformer.transform(originalBytes,
                "ai/narrativetrace/agent/sample/Calculator");

        var loader = new ByteArrayClassLoader(getClass().getClassLoader(), transformed,
                "ai.narrativetrace.agent.sample.Calculator");
        var clazz = loader.loadClass("ai.narrativetrace.agent.sample.Calculator");
        var instance = clazz.getDeclaredConstructor().newInstance();
        clazz.getMethod("add", int.class, int.class).invoke(instance, 3, 4);

        var tree = context.captureTrace();
        var root = tree.roots().get(0);
        assertThat(root.signature().parameters()).containsExactly(
                new ParameterCapture("a", "3", false),
                new ParameterCapture("b", "4", false));
    }

    @Test
    void transformedClassResolvesNarratedTemplate() throws Exception {
        var originalBytes = getClass().getClassLoader()
                .getResourceAsStream("ai/narrativetrace/agent/sample/AnnotatedService.class")
                .readAllBytes();

        var transformed = ClassTransformer.transform(originalBytes,
                "ai/narrativetrace/agent/sample/AnnotatedService");

        var loader = new ByteArrayClassLoader(getClass().getClassLoader(), transformed,
                "ai.narrativetrace.agent.sample.AnnotatedService");
        var clazz = loader.loadClass("ai.narrativetrace.agent.sample.AnnotatedService");
        var instance = clazz.getDeclaredConstructor().newInstance();
        clazz.getMethod("calculatePrice", String.class, int.class).invoke(instance, "widget", 5);

        var tree = context.captureTrace();
        var root = tree.roots().get(0);
        assertThat(root.signature().narration()).isEqualTo("Processing widget for quantity 5");
    }

    @Test
    void transformedClassResolvesOnErrorContext() throws Exception {
        var originalBytes = getClass().getClassLoader()
                .getResourceAsStream("ai/narrativetrace/agent/sample/AnnotatedService.class")
                .readAllBytes();

        var transformed = ClassTransformer.transform(originalBytes,
                "ai/narrativetrace/agent/sample/AnnotatedService");

        var loader = new ByteArrayClassLoader(getClass().getClassLoader(), transformed,
                "ai.narrativetrace.agent.sample.AnnotatedService");
        var clazz = loader.loadClass("ai.narrativetrace.agent.sample.AnnotatedService");
        var instance = clazz.getDeclaredConstructor().newInstance();

        try {
            clazz.getMethod("transfer", String.class, String.class, int.class)
                    .invoke(instance, "alice", "bob", -50);
        } catch (Exception e) {
            // expected
        }

        var tree = context.captureTrace();
        var root = tree.roots().get(0);
        assertThat(root.outcome()).isInstanceOf(TraceOutcome.Threw.class);
        assertThat(root.signature().errorContext()).isEqualTo("Transfer failed for amount -50");
    }

    @Test
    void transformedClassRedactsNotTracedParameter() throws Exception {
        var originalBytes = getClass().getClassLoader()
                .getResourceAsStream("ai/narrativetrace/agent/sample/AnnotatedService.class")
                .readAllBytes();

        var transformed = ClassTransformer.transform(originalBytes,
                "ai/narrativetrace/agent/sample/AnnotatedService");

        var loader = new ByteArrayClassLoader(getClass().getClassLoader(), transformed,
                "ai.narrativetrace.agent.sample.AnnotatedService");
        var clazz = loader.loadClass("ai.narrativetrace.agent.sample.AnnotatedService");
        var instance = clazz.getDeclaredConstructor().newInstance();
        clazz.getMethod("login", String.class, String.class).invoke(instance, "alice", "secret123");

        var tree = context.captureTrace();
        var root = tree.roots().get(0);
        assertThat(root.signature().parameters()).containsExactly(
                new ParameterCapture("username", "\"alice\"", false),
                new ParameterCapture("password", "[REDACTED]", true));
    }

    @Test
    void singleOnErrorAnnotationResolvesContext() throws Exception {
        var originalBytes = getClass().getClassLoader()
                .getResourceAsStream("ai/narrativetrace/agent/sample/AnnotatedService.class")
                .readAllBytes();

        var transformed = ClassTransformer.transform(originalBytes,
                "ai/narrativetrace/agent/sample/AnnotatedService");

        var loader = new ByteArrayClassLoader(getClass().getClassLoader(), transformed,
                "ai.narrativetrace.agent.sample.AnnotatedService");
        var clazz = loader.loadClass("ai.narrativetrace.agent.sample.AnnotatedService");
        var instance = clazz.getDeclaredConstructor().newInstance();

        try {
            clazz.getMethod("lookup", String.class).invoke(instance, (Object) null);
        } catch (Exception e) {
            // expected
        }

        var tree = context.captureTrace();
        var root = tree.roots().get(0);
        assertThat(root.outcome()).isInstanceOf(TraceOutcome.Threw.class);
        assertThat(root.signature().errorContext()).startsWith("Lookup failed for id ");
    }

    @Test
    void methodWithoutParameterNamesUsesSimpleEnterMethod() throws Exception {
        // Generate a class via ASM without -parameters or debug info
        var cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "test/NoParams", null, "java/lang/Object", null);
        // Constructor
        var init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(1, 1);
        init.visitEnd();
        // Method with params but no MethodParameters attribute and no LocalVariableTable
        var mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "compute", "(II)I", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ILOAD, 1);
        mv.visitVarInsn(Opcodes.ILOAD, 2);
        mv.visitInsn(Opcodes.IADD);
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitMaxs(2, 3);
        mv.visitEnd();
        cw.visitEnd();
        var originalBytes = cw.toByteArray();

        var transformed = ClassTransformer.transform(originalBytes, "test/NoParams");

        var loader = new ByteArrayClassLoader(getClass().getClassLoader(), transformed, "test.NoParams");
        var clazz = loader.loadClass("test.NoParams");
        var instance = clazz.getDeclaredConstructor().newInstance();
        var result = clazz.getMethod("compute", int.class, int.class).invoke(instance, 5, 3);

        assertThat(result).isEqualTo(8);
        var tree = context.captureTrace();
        assertThat(tree.roots()).hasSize(1);
        assertThat(tree.roots().get(0).signature().parameters()).isEmpty();
    }

    @Test
    void untransformedClassesPassThrough() {
        var config = AgentConfig.parse("packages=ai.narrativetrace.test");

        assertThat(config.shouldTransform("java/lang/String")).isFalse();
        assertThat(config.shouldTransform("com/other/Service")).isFalse();
    }

    @Test
    void transformedClassTracesAllReturnTypes() throws Exception {
        var originalBytes = getClass().getClassLoader()
                .getResourceAsStream("ai/narrativetrace/agent/sample/TypeVariety.class")
                .readAllBytes();

        var transformed = ClassTransformer.transform(originalBytes,
                "ai/narrativetrace/agent/sample/TypeVariety");

        var loader = new ByteArrayClassLoader(getClass().getClassLoader(), transformed,
                "ai.narrativetrace.agent.sample.TypeVariety");
        var clazz = loader.loadClass("ai.narrativetrace.agent.sample.TypeVariety");
        var instance = clazz.getDeclaredConstructor().newInstance();

        // boolean
        clazz.getMethod("isReady").invoke(instance);
        var tree = context.captureTrace();
        assertThat(tree.roots()).hasSize(1);
        assertThat(((TraceOutcome.Returned) tree.roots().get(0).outcome()).renderedValue()).isEqualTo("true");
        context.reset();

        // long
        clazz.getMethod("timestamp").invoke(instance);
        tree = context.captureTrace();
        assertThat(((TraceOutcome.Returned) tree.roots().get(0).outcome()).renderedValue()).isEqualTo("1234567890");
        context.reset();

        // double
        clazz.getMethod("ratio").invoke(instance);
        tree = context.captureTrace();
        assertThat(((TraceOutcome.Returned) tree.roots().get(0).outcome()).renderedValue()).isEqualTo("3.14");
        context.reset();

        // float
        clazz.getMethod("weight").invoke(instance);
        tree = context.captureTrace();
        assertThat(((TraceOutcome.Returned) tree.roots().get(0).outcome()).renderedValue()).isEqualTo("2.5");
        context.reset();

        // void
        clazz.getMethod("doNothing").invoke(instance);
        tree = context.captureTrace();
        assertThat(((TraceOutcome.Returned) tree.roots().get(0).outcome()).renderedValue()).isEqualTo("null");
        context.reset();

        // String (ARETURN)
        clazz.getMethod("greeting").invoke(instance);
        tree = context.captureTrace();
        assertThat(((TraceOutcome.Returned) tree.roots().get(0).outcome()).renderedValue()).isEqualTo("\"hello\"");
        context.reset();

        // char
        clazz.getMethod("initial").invoke(instance);
        tree = context.captureTrace();
        assertThat(((TraceOutcome.Returned) tree.roots().get(0).outcome()).renderedValue()).isEqualTo("A");
        context.reset();

        // short
        clazz.getMethod("shortValue").invoke(instance);
        tree = context.captureTrace();
        assertThat(((TraceOutcome.Returned) tree.roots().get(0).outcome()).renderedValue()).isEqualTo("42");
        context.reset();

        // byte
        clazz.getMethod("byteValue").invoke(instance);
        tree = context.captureTrace();
        assertThat(((TraceOutcome.Returned) tree.roots().get(0).outcome()).renderedValue()).isEqualTo("7");
    }

    @Test
    void allPrimitiveParameterTypesAreCaptured() throws Exception {
        var originalBytes = getClass().getClassLoader()
                .getResourceAsStream("ai/narrativetrace/agent/sample/TypeVariety.class")
                .readAllBytes();

        var transformed = ClassTransformer.transform(originalBytes,
                "ai/narrativetrace/agent/sample/TypeVariety");

        var loader = new ByteArrayClassLoader(getClass().getClassLoader(), transformed,
                "ai.narrativetrace.agent.sample.TypeVariety");
        var clazz = loader.loadClass("ai.narrativetrace.agent.sample.TypeVariety");
        var instance = clazz.getDeclaredConstructor().newInstance();
        clazz.getMethod("allPrimitiveParams", boolean.class, char.class, byte.class, short.class, float.class)
                .invoke(instance, true, 'X', (byte) 3, (short) 7, 1.5f);

        var tree = context.captureTrace();
        var root = tree.roots().get(0);
        assertThat(root.signature().parameters()).containsExactly(
                new ParameterCapture("flag", "true", false),
                new ParameterCapture("letter", "X", false),
                new ParameterCapture("b", "3", false),
                new ParameterCapture("s", "7", false),
                new ParameterCapture("f", "1.5", false));
    }

    @Test
    void noParamMethodProducesEmptyParameterList() throws Exception {
        var originalBytes = getClass().getClassLoader()
                .getResourceAsStream("ai/narrativetrace/agent/sample/TypeVariety.class")
                .readAllBytes();

        var transformed = ClassTransformer.transform(originalBytes,
                "ai/narrativetrace/agent/sample/TypeVariety");

        var loader = new ByteArrayClassLoader(getClass().getClassLoader(), transformed,
                "ai.narrativetrace.agent.sample.TypeVariety");
        var clazz = loader.loadClass("ai.narrativetrace.agent.sample.TypeVariety");
        var instance = clazz.getDeclaredConstructor().newInstance();
        clazz.getMethod("doNothing").invoke(instance);

        var tree = context.captureTrace();
        var root = tree.roots().get(0);
        assertThat(root.signature().parameters()).isEmpty();
    }

    @Test
    void wideTypeParametersHaveCorrectSlotCalculation() throws Exception {
        var originalBytes = getClass().getClassLoader()
                .getResourceAsStream("ai/narrativetrace/agent/sample/Calculator.class")
                .readAllBytes();

        var transformed = ClassTransformer.transform(originalBytes,
                "ai/narrativetrace/agent/sample/Calculator");

        var loader = new ByteArrayClassLoader(getClass().getClassLoader(), transformed,
                "ai.narrativetrace.agent.sample.Calculator");
        var clazz = loader.loadClass("ai.narrativetrace.agent.sample.Calculator");
        var instance = clazz.getDeclaredConstructor().newInstance();
        clazz.getMethod("wideParams", long.class, double.class, int.class)
                .invoke(instance, 100L, 2.5, 3);

        var tree = context.captureTrace();
        var root = tree.roots().get(0);
        assertThat(root.signature().parameters()).containsExactly(
                new ParameterCapture("x", "100", false),
                new ParameterCapture("y", "2.5", false),
                new ParameterCapture("z", "3", false));
    }

    @Test
    void staticMethodParametersStartAtSlotZero() throws Exception {
        var originalBytes = getClass().getClassLoader()
                .getResourceAsStream("ai/narrativetrace/agent/sample/Calculator.class")
                .readAllBytes();

        var transformed = ClassTransformer.transform(originalBytes,
                "ai/narrativetrace/agent/sample/Calculator");

        var loader = new ByteArrayClassLoader(getClass().getClassLoader(), transformed,
                "ai.narrativetrace.agent.sample.Calculator");
        var clazz = loader.loadClass("ai.narrativetrace.agent.sample.Calculator");
        var result = clazz.getMethod("staticAdd", int.class, int.class).invoke(null, 10, 20);

        assertThat(result).isEqualTo(30);
        var tree = context.captureTrace();
        var root = tree.roots().get(0);
        assertThat(root.signature().parameters()).containsExactly(
                new ParameterCapture("a", "10", false),
                new ParameterCapture("b", "20", false));
    }

    @Test
    void manyParamsUsesBipushForArrayIndex() throws Exception {
        var originalBytes = getClass().getClassLoader()
                .getResourceAsStream("ai/narrativetrace/agent/sample/TypeVariety.class")
                .readAllBytes();

        var transformed = ClassTransformer.transform(originalBytes,
                "ai/narrativetrace/agent/sample/TypeVariety");

        var loader = new ByteArrayClassLoader(getClass().getClassLoader(), transformed,
                "ai.narrativetrace.agent.sample.TypeVariety");
        var clazz = loader.loadClass("ai.narrativetrace.agent.sample.TypeVariety");
        var instance = clazz.getDeclaredConstructor().newInstance();
        clazz.getMethod("manyParams", int.class, int.class, int.class, int.class,
                        int.class, int.class, int.class)
                .invoke(instance, 1, 2, 3, 4, 5, 6, 7);

        var tree = context.captureTrace();
        var root = tree.roots().get(0);
        assertThat(root.signature().parameters()).hasSize(7);
        assertThat(((TraceOutcome.Returned) root.outcome()).renderedValue()).isEqualTo("28");
    }

    @Test
    void partialParamNamesFallBackToArgIndex() throws Exception {
        // Generate a class with 2 params but only first has LocalVariableTable entry
        var cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "test/PartialNames", null, "java/lang/Object", null);
        var init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(1, 1);
        init.visitEnd();

        var mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "compute", "(II)I", null, null);
        mv.visitCode();
        var start = new org.objectweb.asm.Label();
        var end = new org.objectweb.asm.Label();
        mv.visitLabel(start);
        mv.visitVarInsn(Opcodes.ILOAD, 1);
        mv.visitVarInsn(Opcodes.ILOAD, 2);
        mv.visitInsn(Opcodes.IADD);
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitLabel(end);
        // Only first param has a name in LocalVariableTable
        mv.visitLocalVariable("this", "Ltest/PartialNames;", null, start, end, 0);
        mv.visitLocalVariable("named", "I", null, start, end, 1);
        // Second param (slot 2) has no LocalVariableTable entry
        mv.visitMaxs(2, 3);
        mv.visitEnd();
        cw.visitEnd();
        var originalBytes = cw.toByteArray();

        var transformed = ClassTransformer.transform(originalBytes, "test/PartialNames");

        var loader = new ByteArrayClassLoader(getClass().getClassLoader(), transformed, "test.PartialNames");
        var clazz = loader.loadClass("test.PartialNames");
        var instance = clazz.getDeclaredConstructor().newInstance();
        clazz.getMethod("compute", int.class, int.class).invoke(instance, 5, 3);

        var tree = context.captureTrace();
        var root = tree.roots().get(0);
        assertThat(root.signature().parameters()).hasSize(2);
        assertThat(root.signature().parameters().get(0).name()).isEqualTo("named");
        assertThat(root.signature().parameters().get(1).name()).isEqualTo("arg1");
    }

    @Test
    void wideParamsResolvedViaLocalVariableTable() throws Exception {
        // Generate a class with long/double params, debug info (LocalVariableTable) but no MethodParameters
        var cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "test/WideLocals", null, "java/lang/Object", null);
        var init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(1, 1);
        init.visitEnd();

        // method: long process(long amount, double rate, int count) — uses debug info only
        var mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "process", "(JDI)J", null, null);
        mv.visitCode();
        var start = new org.objectweb.asm.Label();
        var end = new org.objectweb.asm.Label();
        mv.visitLabel(start);
        mv.visitVarInsn(Opcodes.LLOAD, 1);   // amount (slot 1-2, wide)
        mv.visitVarInsn(Opcodes.DLOAD, 3);   // rate (slot 3-4, wide)
        mv.visitInsn(Opcodes.D2L);
        mv.visitInsn(Opcodes.LADD);
        mv.visitVarInsn(Opcodes.ILOAD, 5);   // count (slot 5)
        mv.visitInsn(Opcodes.I2L);
        mv.visitInsn(Opcodes.LADD);
        mv.visitInsn(Opcodes.LRETURN);
        mv.visitLabel(end);
        // LocalVariableTable entries (no MethodParameters attribute)
        mv.visitLocalVariable("this", "Ltest/WideLocals;", null, start, end, 0);
        mv.visitLocalVariable("amount", "J", null, start, end, 1);
        mv.visitLocalVariable("rate", "D", null, start, end, 3);
        mv.visitLocalVariable("count", "I", null, start, end, 5);
        mv.visitMaxs(4, 6);
        mv.visitEnd();
        cw.visitEnd();
        var originalBytes = cw.toByteArray();

        var transformed = ClassTransformer.transform(originalBytes, "test/WideLocals");

        var loader = new ByteArrayClassLoader(getClass().getClassLoader(), transformed, "test.WideLocals");
        var clazz = loader.loadClass("test.WideLocals");
        var instance = clazz.getDeclaredConstructor().newInstance();
        var result = clazz.getMethod("process", long.class, double.class, int.class)
                .invoke(instance, 100L, 2.5, 3);

        assertThat(result).isEqualTo(105L);
        var tree = context.captureTrace();
        var root = tree.roots().get(0);
        assertThat(root.signature().parameters()).containsExactly(
                new ParameterCapture("amount", "100", false),
                new ParameterCapture("rate", "2.5", false),
                new ParameterCapture("count", "3", false));
    }

    @Test
    void constructorsAndStaticInitializersAreNotInstrumented() throws Exception {
        var originalBytes = getClass().getClassLoader()
                .getResourceAsStream("ai/narrativetrace/agent/sample/StaticInit.class")
                .readAllBytes();

        var transformed = ClassTransformer.transform(originalBytes,
                "ai/narrativetrace/agent/sample/StaticInit");

        var loader = new ByteArrayClassLoader(getClass().getClassLoader(), transformed,
                "ai.narrativetrace.agent.sample.StaticInit");
        var clazz = loader.loadClass("ai.narrativetrace.agent.sample.StaticInit");
        var instance = clazz.getDeclaredConstructor().newInstance();
        clazz.getMethod("value").invoke(instance);

        var tree = context.captureTrace();
        assertThat(tree.roots()).hasSize(1);
        assertThat(tree.roots().get(0).signature().methodName()).isEqualTo("value");
    }
}
