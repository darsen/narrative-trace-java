package ai.narrativetrace.core.export;

/** HTTP request metadata for trace export: method, URI, status code, and duration. */
public record RequestContext(String method, String uri, int statusCode, long durationMillis) {}
