package ai.narrativetrace.agent;

import ai.narrativetrace.agent.MethodMetadata.OnErrorEntry;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;

final class ErrorHandlerBuilder extends BytecodeBuilder {

    ErrorHandlerBuilder(MethodVisitor mv) {
        super(mv);
    }

    void emit(MethodMetadata.OnErrorEntry[] onErrors,
              int paramNamesLocal, int paramValuesLocal, int redactedLocal) {
        mv.visitInsn(Opcodes.DUP);
        mv.visitInsn(Opcodes.DUP);

        emitStringArray(Arrays.stream(onErrors).map(OnErrorEntry::template).toArray(String[]::new));
        emitStringArray(Arrays.stream(onErrors).map(OnErrorEntry::exceptionDescriptor).toArray(String[]::new));

        mv.visitVarInsn(Opcodes.ALOAD, paramNamesLocal);
        mv.visitVarInsn(Opcodes.ALOAD, paramValuesLocal);
        mv.visitVarInsn(Opcodes.ALOAD, redactedLocal);

        callAgentRuntime("resolveErrorContext",
                "(Ljava/lang/Throwable;[Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/Object;[Z)Ljava/lang/String;");
        callAgentRuntime("exitMethodWithException",
                "(Ljava/lang/Throwable;Ljava/lang/String;)V");
    }
}
