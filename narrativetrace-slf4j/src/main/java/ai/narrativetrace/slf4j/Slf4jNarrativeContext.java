package ai.narrativetrace.slf4j;

import ai.narrativetrace.core.context.ContextSnapshot;
import ai.narrativetrace.core.context.NarrativeContext;
import ai.narrativetrace.core.event.MethodSignature;
import ai.narrativetrace.core.tree.TraceTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.event.Level;

import java.util.Map;
import java.util.stream.Collectors;

public final class Slf4jNarrativeContext implements NarrativeContext {

    public enum EventType { ENTRY, RETURN, EXCEPTION }

    private static final Logger logger = LoggerFactory.getLogger("narrativetrace");

    private final NarrativeContext delegate;
    private final ThreadLocal<Integer> depth = ThreadLocal.withInitial(() -> 0);
    private final Level entryLevel;
    private final Level returnLevel;
    private final Level exceptionLevel;

    public Slf4jNarrativeContext(NarrativeContext delegate) {
        this(delegate, Map.of());
    }

    public Slf4jNarrativeContext(NarrativeContext delegate, Map<EventType, Level> levelMappings) {
        this.delegate = delegate;
        this.entryLevel = levelMappings.getOrDefault(EventType.ENTRY, Level.TRACE);
        this.returnLevel = levelMappings.getOrDefault(EventType.RETURN, Level.TRACE);
        this.exceptionLevel = levelMappings.getOrDefault(EventType.EXCEPTION, Level.WARN);
    }

    @Override
    public boolean isActive() { return delegate.isActive(); }

    @Override
    public void enterMethod(MethodSignature signature) {
        int currentDepth = depth.get();
        depth.set(currentDepth + 1);

        if (logger.isEnabledForLevel(entryLevel)) {
            MDC.put("nt.class", signature.className());
            MDC.put("nt.method", signature.methodName());
            MDC.put("nt.depth", String.valueOf(currentDepth + 1));
            try {
                logger.atLevel(entryLevel).log("→ {}.{}({})", signature.className(), signature.methodName(),
                        formatParams(signature));
            } finally {
                MDC.remove("nt.class");
                MDC.remove("nt.method");
                MDC.remove("nt.depth");
            }
        }
        delegate.enterMethod(signature);
    }

    @Override
    public void exitMethodWithReturn(String renderedReturnValue) {
        depth.set(Math.max(0, depth.get() - 1));
        if (logger.isEnabledForLevel(returnLevel)) {
            logger.atLevel(returnLevel).log("← returned: {}", renderedReturnValue);
        }
        delegate.exitMethodWithReturn(renderedReturnValue);
    }

    @Override
    public void exitMethodWithException(Throwable exception, String errorContext) {
        depth.set(Math.max(0, depth.get() - 1));
        if (errorContext != null) {
            logger.atLevel(exceptionLevel).log("!! {}: {} [{}]",
                    exception.getClass().getSimpleName(), exception.getMessage(), errorContext);
        } else {
            logger.atLevel(exceptionLevel).log("!! {}: {}",
                    exception.getClass().getSimpleName(), exception.getMessage());
        }
        delegate.exitMethodWithException(exception, errorContext);
    }

    @Override
    public TraceTree captureTrace() {
        return delegate.captureTrace();
    }

    @Override
    public void reset() {
        delegate.reset();
    }

    @Override
    public ContextSnapshot snapshot() {
        return delegate.snapshot();
    }

    private String formatParams(MethodSignature signature) {
        return signature.parameters().stream()
                .map(p -> p.name() + ": " + (p.redacted() ? "[REDACTED]" : p.renderedValue()))
                .collect(Collectors.joining(", "));
    }
}
