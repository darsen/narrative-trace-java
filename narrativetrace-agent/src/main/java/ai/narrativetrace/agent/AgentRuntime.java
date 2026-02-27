package ai.narrativetrace.agent;

import ai.narrativetrace.core.context.NarrativeContext;
import ai.narrativetrace.core.context.ThreadLocalNarrativeContext;
import ai.narrativetrace.core.event.MethodSignature;
import ai.narrativetrace.core.event.ParameterCapture;
import ai.narrativetrace.core.render.ValueRenderer;
import java.util.ArrayList;
import java.util.List;

/** Runtime support for the Java agent providing the shared NarrativeContext and ValueRenderer. */
public final class AgentRuntime {

  private static final ValueRenderer VALUE_RENDERER = new ValueRenderer();
  private static volatile NarrativeContext context = new ThreadLocalNarrativeContext();

  private AgentRuntime() {}

  public static void setContext(NarrativeContext ctx) {
    context = ctx;
  }

  public static NarrativeContext getContext() {
    return context;
  }

  public static void enterMethod(String className, String methodName) {
    context.enterMethod(new MethodSignature(className, methodName, List.of()));
  }

  public static void enterMethod(
      String className,
      String methodName,
      String[] paramNames,
      Object[] paramValues,
      boolean[] redacted,
      String narratedTemplate) {
    if (!context.isActive()) return;
    var narration = resolveNarration(narratedTemplate, paramNames, paramValues, redacted);
    var captures = buildCaptures(paramNames, paramValues, redacted);
    context.enterMethod(
        new MethodSignature(className, methodName, List.copyOf(captures), narration, null));
  }

  private static List<ParameterCapture> buildCaptures(
      String[] paramNames, Object[] paramValues, boolean[] redacted) {
    var captures = new ArrayList<ParameterCapture>(paramNames.length);
    for (int i = 0; i < paramNames.length; i++) {
      var rendered = redacted[i] ? "[REDACTED]" : VALUE_RENDERER.render(paramValues[i]);
      captures.add(new ParameterCapture(paramNames[i], rendered, redacted[i]));
    }
    return captures;
  }

  private static String resolveNarration(
      String template, String[] paramNames, Object[] paramValues, boolean[] redacted) {
    if (template == null) {
      return null;
    }
    var valueMap = new java.util.LinkedHashMap<String, Object>();
    for (int i = 0; i < paramNames.length; i++) {
      valueMap.put(paramNames[i], redacted[i] ? "[REDACTED]" : paramValues[i]);
    }
    return ai.narrativetrace.core.template.TemplateParser.resolve(template, valueMap);
  }

  public static void exitMethodWithReturn(Object returnValue) {
    if (!context.isActive()) return;
    context.exitMethodWithReturn(VALUE_RENDERER.render(returnValue));
  }

  public static void exitMethodWithException(Throwable exception) {
    context.exitMethodWithException(exception, null);
  }

  public static void exitMethodWithException(Throwable exception, String errorContext) {
    context.exitMethodWithException(exception, errorContext);
  }

  public static String resolveErrorContext(
      Throwable exception,
      String[] templates,
      String[] exceptionDescriptors,
      String[] paramNames,
      Object[] paramValues,
      boolean[] redacted) {
    var bestTemplate = findBestTemplate(exception, templates, exceptionDescriptors);
    if (bestTemplate == null) return null;
    return resolveNarration(bestTemplate, paramNames, paramValues, redacted);
  }

  private static String findBestTemplate(
      Throwable exception, String[] templates, String[] exceptionDescriptors) {
    String bestTemplate = null;
    Class<?> bestExceptionType = null;
    for (int i = 0; i < templates.length; i++) {
      var exceptionType = descriptorToClass(exceptionDescriptors[i]);
      if (exceptionType != null && exceptionType.isInstance(exception)) {
        if (bestExceptionType == null || bestExceptionType.isAssignableFrom(exceptionType)) {
          bestTemplate = templates[i];
          bestExceptionType = exceptionType;
        }
      }
    }
    return bestTemplate;
  }

  private static Class<?> descriptorToClass(String descriptor) {
    // Convert "Ljava/lang/IllegalArgumentException;" to "java.lang.IllegalArgumentException"
    if (descriptor.startsWith("L") && descriptor.endsWith(";")) {
      var className = descriptor.substring(1, descriptor.length() - 1).replace('/', '.');
      try {
        return Class.forName(className);
      } catch (ClassNotFoundException e) {
        return null;
      }
    }
    return null;
  }
}
