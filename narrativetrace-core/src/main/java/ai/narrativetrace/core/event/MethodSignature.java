package ai.narrativetrace.core.event;

import java.util.List;

public record MethodSignature(String className, String methodName, List<ParameterCapture> parameters,
                              String narration, String errorContext) {

    public MethodSignature(String className, String methodName, List<ParameterCapture> parameters) {
        this(className, methodName, parameters, null, null);
    }
}
