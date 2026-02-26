package ai.narrativetrace.core.config;

public enum TracingLevel {

    OFF,
    ERRORS,
    SUMMARY,
    NARRATIVE,
    DETAIL;

    public boolean isEnabled(TracingLevel required) {
        return this.ordinal() >= required.ordinal();
    }
}
