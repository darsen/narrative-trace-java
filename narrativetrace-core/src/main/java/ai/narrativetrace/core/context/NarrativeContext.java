package ai.narrativetrace.core.context;

import ai.narrativetrace.core.event.MethodSignature;
import ai.narrativetrace.core.tree.TraceTree;

/**
 * Central interface for recording method entries, exits, and exceptions during trace capture.
 *
 * <p>A {@code NarrativeContext} maintains a call stack of method invocations. The proxy or agent
 * calls {@link #enterMethod} on entry and {@link #exitMethodWithReturn} or {@link
 * #exitMethodWithException} on exit. After the test or request completes, {@link #captureTrace()}
 * returns the immutable trace tree.
 *
 * <p><strong>Threading model:</strong> Implementations must be safe for use from a single thread.
 * For cross-thread propagation, use {@link #snapshot()} to create a {@link ContextSnapshot} that
 * can be activated on another thread.
 *
 * <p><strong>Lifecycle:</strong> Call {@link #reset()} between tests or requests to clear
 * accumulated state.
 *
 * <p>Implementations:
 *
 * <ul>
 *   <li>{@link ThreadLocalNarrativeContext} — default, zero-dependency, ThreadLocal-based
 *   <li>{@code ai.narrativetrace.slf4j.Slf4jNarrativeContext} — routes events through SLF4J with
 *       MDC
 * </ul>
 *
 * @see ThreadLocalNarrativeContext
 * @see ContextSnapshot
 */
public interface NarrativeContext {

  /**
   * Returns whether this context is actively recording traces.
   *
   * <p>When {@code false}, proxies and agents can skip capture entirely for zero-overhead
   * passthrough. The default implementation returns {@code true}.
   *
   * @return {@code true} if tracing is active
   */
  default boolean isActive() {
    return true;
  }

  /**
   * Records entry into a method.
   *
   * <p>Pushes a new frame onto the call stack. The proxy or agent must call a corresponding exit
   * method ({@link #exitMethodWithReturn} or {@link #exitMethodWithException}) for every {@code
   * enterMethod} call.
   *
   * @param signature the method being entered, including class name, method name, captured
   *     parameters, and any annotation-based narration
   */
  void enterMethod(MethodSignature signature);

  /**
   * Records a normal method return.
   *
   * @param renderedReturnValue the return value pre-rendered to a String via {@code ValueRenderer},
   *     or {@code null} for void methods
   */
  void exitMethodWithReturn(String renderedReturnValue);

  /**
   * Records a method exit via exception.
   *
   * @param exception the thrown exception
   * @param errorContext the resolved {@code @OnError} template, or {@code null} if no matching
   *     template was found
   */
  void exitMethodWithException(Throwable exception, String errorContext);

  /**
   * Returns the immutable trace tree accumulated since the last {@link #reset()}.
   *
   * @return the captured trace tree
   */
  TraceTree captureTrace();

  /** Clears all accumulated trace state. Call between tests or requests. */
  void reset();

  /**
   * Creates a snapshot for cross-thread trace propagation.
   *
   * <p>The returned {@link ContextSnapshot} can be passed to another thread and {@linkplain
   * ContextSnapshot#activate() activated} to continue the trace there.
   *
   * @return a snapshot of the current context state
   */
  ContextSnapshot snapshot();
}
