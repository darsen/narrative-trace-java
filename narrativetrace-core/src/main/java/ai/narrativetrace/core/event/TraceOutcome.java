package ai.narrativetrace.core.event;

public sealed interface TraceOutcome {

    record Returned(String renderedValue) implements TraceOutcome {
    }

    record Threw(Throwable exception) implements TraceOutcome {
    }
}
