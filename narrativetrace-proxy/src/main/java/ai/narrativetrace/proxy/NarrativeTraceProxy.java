package ai.narrativetrace.proxy;

import ai.narrativetrace.core.annotation.Narrated;
import ai.narrativetrace.core.annotation.NotTraced;
import ai.narrativetrace.core.annotation.OnError;
import ai.narrativetrace.core.context.NarrativeContext;
import ai.narrativetrace.core.event.MethodSignature;
import ai.narrativetrace.core.render.ValueRenderer;
import ai.narrativetrace.core.template.TemplateParser;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Creates JDK dynamic proxies that automatically capture trace events on interface method calls.
 *
 * <p>This is the primary entry point for adding tracing to any interface-based service. The proxy
 * intercepts every method call, records parameters and return values via the given {@link
 * NarrativeContext}, and delegates to the real implementation.
 *
 * <pre>{@code
 * var context = new ThreadLocalNarrativeContext();
 * OrderService traced = NarrativeTraceProxy.trace(realService, OrderService.class, context);
 * traced.placeOrder("item-1", 3);  // automatically traced
 * TraceTree tree = context.captureTrace();
 * }</pre>
 *
 * <p><strong>Proxy vs Agent:</strong> Use the proxy when you control the instantiation point and
 * the target implements an interface. Use the Java agent ({@code narrativetrace-agent}) when you
 * need to trace concrete classes or third-party code without source changes.
 *
 * <p>Parameter values are eagerly serialized via the shared {@code VALUE_RENDERER} at capture time.
 * {@code @Narrated} and {@code @OnError} templates are resolved against raw objects before
 * serialization.
 *
 * <p>Requires the {@code -parameters} compiler flag for meaningful parameter names.
 *
 * @see ai.narrativetrace.core.context.NarrativeContext
 * @see ai.narrativetrace.core.annotation.Narrated
 * @see ai.narrativetrace.core.annotation.NotTraced
 */
public final class NarrativeTraceProxy {

  private static final ValueRenderer VALUE_RENDERER = new ValueRenderer();
  private static final ConcurrentHashMap<Method, ProxyMethodMetadata> METHOD_CACHE =
      new ConcurrentHashMap<>();

  private NarrativeTraceProxy() {}

  record ProxyMethodMetadata(
      String[] paramNames, boolean[] redacted, String narratedTemplate, OnError[] onErrors) {}

  static ProxyMethodMetadata computeMetadata(Method method) {
    var parameters = method.getParameters();
    var paramNames = new String[parameters.length];
    var redacted = new boolean[parameters.length];
    for (int i = 0; i < parameters.length; i++) {
      paramNames[i] = parameters[i].getName();
      redacted[i] = parameters[i].isAnnotationPresent(NotTraced.class);
    }
    var narrated = method.getAnnotation(Narrated.class);
    var narratedTemplate = narrated != null ? narrated.value() : null;
    var onErrors = method.getAnnotationsByType(OnError.class);
    return new ProxyMethodMetadata(paramNames, redacted, narratedTemplate, onErrors);
  }

  /**
   * Wraps the target in a tracing proxy for a single interface.
   *
   * @param target the real implementation to delegate to
   * @param interfaceType the interface to proxy (must be an interface)
   * @param context the narrative context to record trace events into
   * @param <T> the interface type
   * @return a proxy implementing {@code interfaceType} that traces all method calls
   */
  @SuppressWarnings("unchecked")
  public static <T> T trace(T target, Class<T> interfaceType, NarrativeContext context) {
    return (T)
        Proxy.newProxyInstance(
            interfaceType.getClassLoader(),
            new Class<?>[] {interfaceType},
            (proxy, method, args) -> {
              // setAccessible needed: target may be a lambda or non-public class
              method.setAccessible(true);
              if (!context.isActive()) {
                return invokeRaw(method, target, args);
              }
              var meta = METHOD_CACHE.computeIfAbsent(method, NarrativeTraceProxy::computeMetadata);
              var safeArgs = args != null ? args : new Object[0];
              var narration = resolveNarration(meta, safeArgs);
              var captures =
                  ParameterNameResolver.resolve(
                      meta.paramNames, meta.redacted, safeArgs, VALUE_RENDERER);
              var signature =
                  new MethodSignature(
                      interfaceType.getSimpleName(), method.getName(), captures, narration, null);
              context.enterMethod(signature);
              try {
                var result = method.invoke(target, args);
                context.exitMethodWithReturn(VALUE_RENDERER.render(result));
                return result;
              } catch (Exception e) {
                var cause = e instanceof InvocationTargetException ite ? ite.getCause() : e;
                var errorContext = resolveErrorContext(meta, safeArgs, cause);
                context.exitMethodWithException(cause, errorContext);
                throw cause;
              }
            });
  }

  /**
   * Wraps the target in a tracing proxy for multiple interfaces.
   *
   * @param target the real implementation to delegate to
   * @param interfaces the interfaces to proxy (at least one required)
   * @param context the narrative context to record trace events into
   * @return a proxy implementing all given interfaces that traces all method calls
   * @throws IllegalArgumentException if {@code interfaces} is empty
   */
  public static Object trace(Object target, Class<?>[] interfaces, NarrativeContext context) {
    if (interfaces.length == 0) {
      throw new IllegalArgumentException("At least one interface is required");
    }
    return Proxy.newProxyInstance(
        interfaces[0].getClassLoader(),
        interfaces,
        (proxy, method, args) -> {
          // setAccessible needed: target may be a lambda or non-public class
          method.setAccessible(true);
          if (!context.isActive()) {
            return invokeRaw(method, target, args);
          }
          var meta = METHOD_CACHE.computeIfAbsent(method, NarrativeTraceProxy::computeMetadata);
          var safeArgs = args != null ? args : new Object[0];
          var narration = resolveNarration(meta, safeArgs);
          var captures =
              ParameterNameResolver.resolve(
                  meta.paramNames, meta.redacted, safeArgs, VALUE_RENDERER);
          var className = method.getDeclaringClass().getSimpleName();
          var signature =
              new MethodSignature(className, method.getName(), captures, narration, null);
          context.enterMethod(signature);
          try {
            var result = method.invoke(target, args);
            context.exitMethodWithReturn(VALUE_RENDERER.render(result));
            return result;
          } catch (Exception e) {
            var cause = e instanceof InvocationTargetException ite ? ite.getCause() : e;
            var errorContext = resolveErrorContext(meta, safeArgs, cause);
            context.exitMethodWithException(cause, errorContext);
            throw cause;
          }
        });
  }

  private static Object invokeRaw(Method method, Object target, Object[] args) throws Throwable {
    try {
      return method.invoke(target, args);
    } catch (InvocationTargetException ite) {
      throw ite.getCause();
    }
  }

  private static String resolveNarration(ProxyMethodMetadata meta, Object[] args) {
    if (meta.narratedTemplate == null) {
      return null;
    }
    return TemplateParser.resolve(
        meta.narratedTemplate, buildValueMap(meta.paramNames, meta.redacted, args));
  }

  private static String resolveErrorContext(
      ProxyMethodMetadata meta, Object[] args, Throwable exception) {
    if (meta.onErrors.length == 0) {
      return null;
    }
    OnError bestMatch = null;
    for (var annotation : meta.onErrors) {
      if (annotation.exception().isInstance(exception)) {
        if (bestMatch == null || bestMatch.exception().isAssignableFrom(annotation.exception())) {
          bestMatch = annotation;
        }
      }
    }
    if (bestMatch == null) {
      return null;
    }
    return TemplateParser.resolve(
        bestMatch.value(), buildValueMap(meta.paramNames, meta.redacted, args));
  }

  private static Map<String, Object> buildValueMap(
      String[] paramNames, boolean[] redacted, Object[] args) {
    var map = new LinkedHashMap<String, Object>();
    for (int i = 0; i < paramNames.length; i++) {
      map.put(paramNames[i], redacted[i] ? "[REDACTED]" : args[i]);
    }
    return map;
  }
}
