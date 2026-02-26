package ai.narrativetrace.core.event;

import java.util.List;

public record TraceNode(MethodSignature signature, List<TraceNode> children, TraceOutcome outcome, long durationNanos) {

    public TraceNode(MethodSignature signature, List<TraceNode> children, TraceOutcome outcome) {
        this(signature, children, outcome, 0L);
    }

    public long durationMillis() {
        return durationNanos / 1_000_000;
    }
}
