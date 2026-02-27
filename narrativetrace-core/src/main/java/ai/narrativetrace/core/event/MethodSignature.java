package ai.narrativetrace.core.event;

import java.util.List;

/**
 * Identifies a traced method with its class, name, captured parameters, and optional
 * annotation-derived narration or error context.
 *
 * @param className simple name of the declaring class
 * @param methodName the method name
 * @param parameters captured parameter names and pre-rendered values
 * @param narration resolved {@code @Narrated} template, or {@code null}
 * @param errorContext resolved {@code @OnError} template, or {@code null}
 */
public record MethodSignature(
    String className,
    String methodName,
    List<ParameterCapture> parameters,
    String narration,
    String errorContext) {

  public MethodSignature(String className, String methodName, List<ParameterCapture> parameters) {
    this(className, methodName, parameters, null, null);
  }
}
