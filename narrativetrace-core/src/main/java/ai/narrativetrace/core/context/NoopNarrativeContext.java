package ai.narrativetrace.core.context;

import ai.narrativetrace.core.event.MethodSignature;
import ai.narrativetrace.core.tree.DefaultTraceTree;
import ai.narrativetrace.core.tree.TraceTree;
import java.util.List;

/** No-op context implementation that discards all trace events (for disabled tracing). */
public final class NoopNarrativeContext implements NarrativeContext {

  public static final NoopNarrativeContext INSTANCE = new NoopNarrativeContext();

  private static final TraceTree EMPTY_TREE = new DefaultTraceTree(List.of());

  private NoopNarrativeContext() {}

  @Override
  public boolean isActive() {
    return false;
  }

  @Override
  public void enterMethod(MethodSignature signature) {}

  @Override
  public void exitMethodWithReturn(String renderedReturnValue) {}

  @Override
  public void exitMethodWithException(Throwable exception, String errorContext) {}

  @Override
  public TraceTree captureTrace() {
    return EMPTY_TREE;
  }

  @Override
  public void reset() {}

  @Override
  public ContextSnapshot snapshot() {
    return NoopContextSnapshot.INSTANCE;
  }

  private static final class NoopContextSnapshot implements ContextSnapshot {
    static final NoopContextSnapshot INSTANCE = new NoopContextSnapshot();
    private static final ContextScope NOOP_SCOPE = () -> {};

    @Override
    public ContextScope activate() {
      return NOOP_SCOPE;
    }
  }
}
