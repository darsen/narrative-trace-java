package ai.narrativetrace.agent;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.function.Function;

final class ParamCaptureBuilder extends BytecodeBuilder {

    ParamCaptureBuilder(MethodVisitor mv) {
        super(mv);
    }

    record StoredLocals(int paramNames, int paramValues, int redacted) {}

    StoredLocals emit(String className, String methodName, MethodMetadata metadata,
                      String methodDesc, int methodAccess,
                      Function<Type, Integer> localAllocator) {
        var paramNames = metadata.parameterNames();
        var redacted = metadata.redacted();
        var argTypes = Type.getArgumentTypes(methodDesc);
        boolean isStatic = (methodAccess & Opcodes.ACC_STATIC) != 0;
        int slotOffset = isStatic ? 0 : 1;
        boolean storeForOnError = metadata.onErrors() != null;

        mv.visitLdcInsn(className);
        mv.visitLdcInsn(methodName);

        emitStringArray(resolveParamNames(paramNames));
        int paramNamesLocal = storeLocalIfNeeded(storeForOnError, "[Ljava/lang/String;", localAllocator);

        emitBoxedValueArray(argTypes, slotOffset);
        int paramValuesLocal = storeLocalIfNeeded(storeForOnError, "[Ljava/lang/Object;", localAllocator);

        emitBooleanArray(redacted);
        int redactedLocal = storeLocalIfNeeded(storeForOnError, "[Z", localAllocator);

        if (metadata.narratedTemplate() != null) {
            mv.visitLdcInsn(metadata.narratedTemplate());
        } else {
            mv.visitInsn(Opcodes.ACONST_NULL);
        }

        callAgentRuntime("enterMethod",
                "(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/Object;[ZLjava/lang/String;)V");

        return new StoredLocals(paramNamesLocal, paramValuesLocal, redactedLocal);
    }

    private static String[] resolveParamNames(String[] paramNames) {
        var resolved = new String[paramNames.length];
        for (int i = 0; i < paramNames.length; i++) {
            resolved[i] = paramNames[i] != null ? paramNames[i] : "arg" + i;
        }
        return resolved;
    }

    private int storeLocalIfNeeded(boolean store, String typeDescriptor,
                                   Function<Type, Integer> localAllocator) {
        if (!store) return -1;
        mv.visitInsn(Opcodes.DUP);
        int local = localAllocator.apply(Type.getType(typeDescriptor));
        mv.visitVarInsn(Opcodes.ASTORE, local);
        return local;
    }
}
