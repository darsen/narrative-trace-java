package ai.narrativetrace.micrometer;

import ai.narrativetrace.core.context.ContextScope;
import ai.narrativetrace.core.context.ContextSnapshot;
import ai.narrativetrace.core.context.ThreadLocalNarrativeContext;
import io.micrometer.context.ThreadLocalAccessor;

/**
 * Micrometer ThreadLocalAccessor that propagates NarrativeTrace ContextSnapshot across thread
 * boundaries.
 */
public class NarrativeTraceThreadLocalAccessor implements ThreadLocalAccessor<ContextSnapshot> {

  public static final String KEY = "narrativetrace";

  private final ThreadLocalNarrativeContext context;
  private final ThreadLocal<ContextScope> currentScope = new ThreadLocal<>();

  public NarrativeTraceThreadLocalAccessor(ThreadLocalNarrativeContext context) {
    this.context = context;
  }

  public NarrativeTraceThreadLocalAccessor() {
    this(new ThreadLocalNarrativeContext());
  }

  @Override
  public Object key() {
    return KEY;
  }

  @Override
  public ContextSnapshot getValue() {
    return context.snapshot();
  }

  @Override
  public void setValue(ContextSnapshot snapshot) {
    var previous = currentScope.get();
    if (previous != null) {
      previous.close();
    }
    var scope = snapshot.activate();
    currentScope.set(scope);
  }

  @Override
  public void setValue() {
    var scope = currentScope.get();
    if (scope != null) {
      scope.close();
      currentScope.remove();
    }
  }
}
