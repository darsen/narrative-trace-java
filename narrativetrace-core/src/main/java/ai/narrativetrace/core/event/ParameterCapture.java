package ai.narrativetrace.core.event;

public record ParameterCapture(String name, String renderedValue, boolean redacted) {
}
