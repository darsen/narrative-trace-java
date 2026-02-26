package ai.narrativetrace.core.export;

public record RequestContext(String method, String uri, int statusCode, long durationMillis) {
}
