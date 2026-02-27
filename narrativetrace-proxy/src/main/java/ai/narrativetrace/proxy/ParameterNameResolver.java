package ai.narrativetrace.proxy;

import ai.narrativetrace.core.annotation.NotTraced;
import ai.narrativetrace.core.event.ParameterCapture;
import ai.narrativetrace.core.render.ValueRenderer;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves parameter names and values into {@link ai.narrativetrace.core.event.ParameterCapture}
 * records.
 */
public final class ParameterNameResolver {

  private ParameterNameResolver() {}

  public static List<ParameterCapture> resolve(
      String[] paramNames, boolean[] redacted, Object[] args, ValueRenderer valueRenderer) {
    var captures = new ArrayList<ParameterCapture>(paramNames.length);
    for (int i = 0; i < paramNames.length; i++) {
      var rendered = redacted[i] ? "[REDACTED]" : valueRenderer.render(args[i]);
      captures.add(new ParameterCapture(paramNames[i], rendered, redacted[i]));
    }
    return List.copyOf(captures);
  }

  public static List<ParameterCapture> resolve(
      Method method, Object[] args, ValueRenderer valueRenderer) {
    var parameters = method.getParameters();
    var captures = new ArrayList<ParameterCapture>(parameters.length);
    for (int i = 0; i < parameters.length; i++) {
      boolean redacted = parameters[i].isAnnotationPresent(NotTraced.class);
      var rendered = redacted ? "[REDACTED]" : valueRenderer.render(args[i]);
      captures.add(new ParameterCapture(parameters[i].getName(), rendered, redacted));
    }
    return List.copyOf(captures);
  }
}
