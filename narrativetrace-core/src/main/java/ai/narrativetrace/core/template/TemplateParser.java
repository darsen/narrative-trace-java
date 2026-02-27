package ai.narrativetrace.core.template;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Resolves {@code {paramName}} and {@code {param.property}} placeholders in annotation templates.
 */
public final class TemplateParser {

  private static final Pattern PLACEHOLDER = Pattern.compile("\\{([^}]+)}");
  private static final ConcurrentHashMap<String, List<Segment>> CACHE = new ConcurrentHashMap<>();

  sealed interface Segment {
    String resolve(Map<String, Object> values);

    record Literal(String text) implements Segment {
      @Override
      public String resolve(Map<String, Object> values) {
        return text;
      }
    }

    record SimplePlaceholder(String key) implements Segment {
      @Override
      public String resolve(Map<String, Object> values) {
        var value = values.get(key);
        return value != null ? String.valueOf(value) : "{" + key + "}";
      }
    }

    record PropertyPlaceholder(String objectKey, String property) implements Segment {
      @Override
      public String resolve(Map<String, Object> values) {
        var propertyValue = accessProperty(values.get(objectKey), property);
        return propertyValue != null
            ? String.valueOf(propertyValue)
            : "{" + objectKey + "." + property + "}";
      }
    }
  }

  private TemplateParser() {}

  public static List<String> findUnresolvedInResult(String resolved) {
    if (resolved == null) {
      return List.of();
    }
    var matcher = PLACEHOLDER.matcher(resolved);
    var unresolved = new ArrayList<String>();
    while (matcher.find()) {
      unresolved.add(matcher.group(1));
    }
    return List.copyOf(unresolved);
  }

  public static String resolve(String template, Map<String, Object> values) {
    var segments = CACHE.computeIfAbsent(template, TemplateParser::parse);
    var sb = new StringBuilder();
    for (var segment : segments) {
      sb.append(segment.resolve(values));
    }
    return sb.toString();
  }

  static List<Segment> parse(String template) {
    var segments = new ArrayList<Segment>();
    var matcher = PLACEHOLDER.matcher(template);
    int lastEnd = 0;
    while (matcher.find()) {
      if (matcher.start() > lastEnd) {
        segments.add(new Segment.Literal(template.substring(lastEnd, matcher.start())));
      }
      segments.add(parsePlaceholder(matcher.group(1)));
      lastEnd = matcher.end();
    }
    if (lastEnd < template.length()) {
      segments.add(new Segment.Literal(template.substring(lastEnd)));
    }
    return List.copyOf(segments);
  }

  private static Segment parsePlaceholder(String key) {
    var dotIndex = key.indexOf('.');
    if (dotIndex >= 0) {
      return new Segment.PropertyPlaceholder(
          key.substring(0, dotIndex), key.substring(dotIndex + 1));
    }
    return new Segment.SimplePlaceholder(key);
  }

  private static Object accessProperty(Object object, String property) {
    if (object == null) {
      return null;
    }
    try {
      // getMethod returns public methods only — setAccessible is unnecessary
      var method = object.getClass().getMethod(property);
      return method.invoke(object);
    } catch (Exception e) {
      // Property access can fail for many reasons: no such method, module
      // encapsulation (InaccessibleObjectException), SecurityException from
      // setAccessible in restricted environments, or the target method itself
      // throwing (wrapped as InvocationTargetException). In all cases, the
      // template should gracefully preserve the {placeholder} text rather
      // than crash trace rendering.
      return unresolvedProperty();
    }
  }

  private static Object unresolvedProperty() {
    return null;
  }
}
