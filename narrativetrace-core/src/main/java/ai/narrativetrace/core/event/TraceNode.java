package ai.narrativetrace.core.event;

import java.util.List;

/**
 * A single method invocation in the trace tree.
 *
 * <p>Each node captures the method signature (class, method, parameters, narration), child
 * invocations, the outcome (returned value or thrown exception), and timing. Nodes form an
 * immutable tree built bottom-up by the context.
 *
 * @param signature the method being invoked
 * @param children nested method calls made during this invocation
 * @param outcome how the method completed (returned or threw)
 * @param durationNanos wall-clock duration in nanoseconds
 */
public record TraceNode(
    MethodSignature signature, List<TraceNode> children, TraceOutcome outcome, long durationNanos) {

  public TraceNode(MethodSignature signature, List<TraceNode> children, TraceOutcome outcome) {
    this(signature, children, outcome, 0L);
  }

  public long durationMillis() {
    return durationNanos / 1_000_000;
  }
}
