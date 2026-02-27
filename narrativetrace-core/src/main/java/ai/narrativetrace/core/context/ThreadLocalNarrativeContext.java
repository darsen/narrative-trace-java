package ai.narrativetrace.core.context;

import ai.narrativetrace.core.config.NarrativeTraceConfig;
import ai.narrativetrace.core.config.TracingLevel;
import ai.narrativetrace.core.event.MethodSignature;
import ai.narrativetrace.core.event.ParameterCapture;
import ai.narrativetrace.core.event.TraceNode;
import ai.narrativetrace.core.event.TraceOutcome;
import ai.narrativetrace.core.tree.DefaultTraceTree;
import ai.narrativetrace.core.tree.TraceTree;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Default {@link NarrativeContext} implementation using a ThreadLocal Deque-based call stack.
 *
 * <p>Each thread maintains an independent call stack. Method entries push frames; exits pop them
 * and build the trace tree bottom-up. The {@link ai.narrativetrace.core.config.TracingLevel}
 * controls what gets captured:
 *
 * <ul>
 *   <li>{@code OFF} — nothing captured, {@link #isActive()} returns {@code false}
 *   <li>{@code ERRORS} — only exception paths recorded
 *   <li>{@code SUMMARY} — root and leaf calls only (intermediate calls pruned)
 *   <li>{@code NARRATIVE} — all calls without parameter values
 *   <li>{@code DETAIL} — all calls with full parameter values
 * </ul>
 *
 * <p>This class has zero external dependencies.
 *
 * <pre>{@code
 * var config = new NarrativeTraceConfig(TracingLevel.DETAIL);
 * var context = new ThreadLocalNarrativeContext(config);
 * var proxy = NarrativeTraceProxy.trace(target, MyService.class, context);
 * }</pre>
 *
 * @see NarrativeContext
 * @see NarrativeTraceConfig
 */
public final class ThreadLocalNarrativeContext implements NarrativeContext {

  private final NarrativeTraceConfig config;
  private final ThreadLocal<TraceStack> stackHolder = ThreadLocal.withInitial(TraceStack::new);

  /** Creates a context with default configuration ({@code TracingLevel.DETAIL}). */
  public ThreadLocalNarrativeContext() {
    this(new NarrativeTraceConfig());
  }

  /**
   * Creates a context with the given configuration.
   *
   * @param config the tracing configuration (level can be changed at runtime)
   */
  public ThreadLocalNarrativeContext(NarrativeTraceConfig config) {
    this.config = config;
  }

  @Override
  public boolean isActive() {
    return config.level().isEnabled(TracingLevel.ERRORS);
  }

  @Override
  public void enterMethod(MethodSignature signature) {
    if (!config.level().isEnabled(TracingLevel.ERRORS)) {
      return;
    }
    if (!config.level().isEnabled(TracingLevel.DETAIL)) {
      signature = suppressParameterValues(signature);
    }
    stackHolder.get().push(signature);
  }

  private MethodSignature suppressParameterValues(MethodSignature signature) {
    var suppressed =
        signature.parameters().stream()
            .map(p -> new ParameterCapture(p.name(), "", p.redacted()))
            .toList();
    return new MethodSignature(
        signature.className(),
        signature.methodName(),
        suppressed,
        signature.narration(),
        signature.errorContext());
  }

  @Override
  public void exitMethodWithReturn(String renderedReturnValue) {
    var traceStack = stackHolder.get();
    if (traceStack.isEmpty()) return;
    var level = config.level();
    if (!level.isEnabled(TracingLevel.ERRORS)) {
      traceStack.discard();
    } else if (level == TracingLevel.ERRORS) {
      traceStack.discard();
    } else if (level == TracingLevel.SUMMARY) {
      var frame = traceStack.peek();
      boolean isLeaf = frame != null && frame.children.isEmpty();
      boolean isRoot = traceStack.size() == 1;
      if (isRoot || isLeaf) {
        traceStack.pop(new TraceOutcome.Returned(renderedReturnValue));
      } else {
        traceStack.discardAndPromoteChildren();
      }
    } else {
      traceStack.pop(new TraceOutcome.Returned(renderedReturnValue));
    }
  }

  @Override
  public void exitMethodWithException(Throwable exception, String errorContext) {
    var traceStack = stackHolder.get();
    if (traceStack.isEmpty()) return;
    if (!config.level().isEnabled(TracingLevel.ERRORS)) {
      traceStack.discard();
      return;
    }
    traceStack.pop(new TraceOutcome.Threw(exception), errorContext);
  }

  @Override
  public TraceTree captureTrace() {
    return new DefaultTraceTree(stackHolder.get().roots());
  }

  @Override
  public void reset() {
    stackHolder.remove();
  }

  @Override
  public ContextSnapshot snapshot() {
    return new ThreadLocalContextSnapshot(this);
  }

  TraceStack swapStack(TraceStack replacement) {
    var previous = stackHolder.get();
    stackHolder.set(replacement);
    return previous;
  }

  private static final class ThreadLocalContextSnapshot implements ContextSnapshot {
    private final ThreadLocalNarrativeContext context;

    ThreadLocalContextSnapshot(ThreadLocalNarrativeContext context) {
      this.context = context;
    }

    @Override
    public ContextScope activate() {
      var previous = context.swapStack(new TraceStack());
      return () -> context.swapStack(previous);
    }
  }

  private static final class TraceStack {
    private final List<TraceNode> roots = new ArrayList<>();
    private final Deque<Frame> stack = new ArrayDeque<>();

    void push(MethodSignature signature) {
      stack.push(new Frame(signature));
    }

    Frame peek() {
      return stack.peek();
    }

    boolean isEmpty() {
      return stack.isEmpty();
    }

    int size() {
      return stack.size();
    }

    void discard() {
      stack.pop();
    }

    void discardAndPromoteChildren() {
      var frame = stack.pop();
      if (!stack.isEmpty()) {
        stack.peek().children.addAll(frame.children);
      }
    }

    void pop(TraceOutcome outcome) {
      pop(outcome, null);
    }

    void pop(TraceOutcome outcome, String errorContext) {
      var frame = stack.pop();
      long durationNanos = System.nanoTime() - frame.entryTimeNanos;
      var signature = frame.signature;
      if (errorContext != null) {
        signature =
            new MethodSignature(
                signature.className(),
                signature.methodName(),
                signature.parameters(),
                signature.narration(),
                errorContext);
      }
      var node = new TraceNode(signature, List.copyOf(frame.children), outcome, durationNanos);
      if (stack.isEmpty()) {
        roots.add(node);
      } else {
        stack.peek().children.add(node);
      }
    }

    List<TraceNode> roots() {
      return List.copyOf(roots);
    }

    private static final class Frame {
      final MethodSignature signature;
      final long entryTimeNanos;
      final List<TraceNode> children = new ArrayList<>();

      Frame(MethodSignature signature) {
        this.signature = signature;
        this.entryTimeNanos = System.nanoTime();
      }
    }
  }
}
