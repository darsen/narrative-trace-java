package ai.narrativetrace.core.context;

import ai.narrativetrace.core.event.MethodSignature;
import ai.narrativetrace.core.tree.TraceTree;

public interface NarrativeContext {

    default boolean isActive() { return true; }

    void enterMethod(MethodSignature signature);

    void exitMethodWithReturn(String renderedReturnValue);

    void exitMethodWithException(Throwable exception, String errorContext);

    TraceTree captureTrace();

    void reset();

    ContextSnapshot snapshot();
}
