package ai.narrativetrace.core.config;

public final class NarrativeTraceConfig {

    private volatile TracingLevel level;

    public NarrativeTraceConfig() {
        this(TracingLevel.DETAIL);
    }

    public NarrativeTraceConfig(TracingLevel level) {
        this.level = level;
    }

    public TracingLevel level() {
        return level;
    }

    public void setLevel(TracingLevel level) {
        this.level = level;
    }
}
