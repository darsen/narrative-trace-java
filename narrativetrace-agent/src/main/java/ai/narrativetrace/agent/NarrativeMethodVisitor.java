package ai.narrativetrace.agent;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

public final class NarrativeMethodVisitor extends AdviceAdapter {

    private final String methodName;
    private final String className;
    private final MethodMetadata metadata;
    private final Label tryStart = new Label();
    private final Label tryEnd = new Label();
    private final Label catchHandler = new Label();
    private final ParamCaptureBuilder paramCaptureBuilder;
    private final ErrorHandlerBuilder errorHandlerBuilder;
    private ParamCaptureBuilder.StoredLocals storedLocals;

    protected NarrativeMethodVisitor(MethodVisitor mv, int access, String name,
                                     String descriptor, String className,
                                     MethodMetadata metadata) {
        super(Opcodes.ASM9, mv, access, name, descriptor);
        this.methodName = name;
        this.className = className.replace('/', '.');
        this.metadata = metadata;
        this.paramCaptureBuilder = new ParamCaptureBuilder(mv);
        this.errorHandlerBuilder = new ErrorHandlerBuilder(mv);
    }

    @Override
    protected void onMethodEnter() {
        if (metadata != null && metadata.parameterNames() != null) {
            storedLocals = paramCaptureBuilder.emit(
                    className, methodName, metadata, methodDesc, methodAccess, this::newLocal);
        } else {
            mv.visitLdcInsn(className);
            mv.visitLdcInsn(methodName);
            paramCaptureBuilder.callAgentRuntime("enterMethod",
                    "(Ljava/lang/String;Ljava/lang/String;)V");
        }

        mv.visitTryCatchBlock(tryStart, tryEnd, catchHandler, "java/lang/Throwable");
        mv.visitLabel(tryStart);
    }

    @Override
    protected void onMethodExit(int opcode) {
        if (opcode != ATHROW) {
            paramCaptureBuilder.boxReturnValue(opcode, methodDesc);
            paramCaptureBuilder.callAgentRuntime("exitMethodWithReturn",
                    "(Ljava/lang/Object;)V");
        }
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        mv.visitLabel(tryEnd);
        mv.visitLabel(catchHandler);

        if (hasOnErrors()) {
            errorHandlerBuilder.emit(metadata.onErrors(),
                    storedLocals.paramNames(), storedLocals.paramValues(), storedLocals.redacted());
        } else {
            mv.visitInsn(Opcodes.DUP);
            errorHandlerBuilder.callAgentRuntime("exitMethodWithException",
                    "(Ljava/lang/Throwable;)V");
        }
        mv.visitInsn(Opcodes.ATHROW);

        super.visitMaxs(maxStack, maxLocals);
    }

    private boolean hasOnErrors() {
        return metadata != null && metadata.onErrors() != null;
    }
}
