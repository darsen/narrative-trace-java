package ai.narrativetrace.core.render;

import ai.narrativetrace.core.annotation.NarrativeSummary;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Serializes objects to string representations with array support, POJO introspection, and cycle
 * detection.
 */
public final class ValueRenderer {

  private static final int DEFAULT_MAX_STRING_LENGTH = 200;
  private static final int DEFAULT_MAX_COLLECTION_ITEMS = 5;
  private static final int DEFAULT_MAX_OBJECT_FIELDS = 5;

  private final int maxStringLength;
  private final int maxCollectionItems;
  private final int maxObjectFields;

  public ValueRenderer() {
    this(DEFAULT_MAX_STRING_LENGTH, DEFAULT_MAX_COLLECTION_ITEMS, DEFAULT_MAX_OBJECT_FIELDS);
  }

  public ValueRenderer(int maxStringLength, int maxCollectionItems, int maxObjectFields) {
    this.maxStringLength = maxStringLength;
    this.maxCollectionItems = maxCollectionItems;
    this.maxObjectFields = maxObjectFields;
  }

  public String render(Object value) {
    if (value == null) {
      return "null";
    }
    if (value instanceof String s) {
      if (s.length() > maxStringLength) {
        return "\"" + s.substring(0, maxStringLength) + "…\"";
      }
      return "\"" + s + "\"";
    }
    if (value instanceof Number
        || value instanceof Boolean
        || value instanceof Character
        || value instanceof Enum<?>) {
      return value.toString();
    }
    return renderComplex(value, Collections.newSetFromMap(new IdentityHashMap<>()));
  }

  private String render(Object value, Set<Object> seen) {
    if (value == null) {
      return "null";
    }
    if (value instanceof String s) {
      if (s.length() > maxStringLength) {
        return "\"" + s.substring(0, maxStringLength) + "…\"";
      }
      return "\"" + s + "\"";
    }
    if (value instanceof Number
        || value instanceof Boolean
        || value instanceof Character
        || value instanceof Enum<?>) {
      return value.toString();
    }
    return renderComplex(value, seen);
  }

  private String renderComplex(Object value, Set<Object> seen) {
    if (value instanceof Collection<?> c) {
      return renderCollection(c, seen);
    }
    if (value.getClass().isArray()) {
      return renderArray(value, seen);
    }
    var summaryMethod = findNarrativeSummaryMethod(value.getClass());
    if (summaryMethod != null) {
      try {
        return String.valueOf(summaryMethod.invoke(value));
      } catch (Exception e) {
        // fall through
      }
    }
    if (value.getClass().isRecord()) {
      return renderRecord(value, seen);
    }
    if (!HAS_CUSTOM_TO_STRING.get(value.getClass())) {
      if (!seen.add(value)) {
        return "<"
            + value.getClass().getSimpleName()
            + "@"
            + Integer.toHexString(System.identityHashCode(value))
            + ">";
      }
      return renderObject(value, seen);
    }
    try {
      return value.toString();
    } catch (Throwable t) { // NOPMD AvoidCatchingThrowable - rogue toString() may throw Error
      return "<" + value.getClass().getSimpleName() + ">";
    }
  }

  private String renderCollection(Collection<?> collection, Set<Object> seen) {
    var sb = new StringBuilder("[");
    var items =
        collection.stream()
            .limit(maxCollectionItems)
            .map(item -> render(item, seen))
            .collect(Collectors.joining(", "));
    sb.append(items);
    if (collection.size() > maxCollectionItems) {
      sb.append(", … (").append(collection.size()).append(" total)");
    }
    sb.append("]");
    return sb.toString();
  }

  private String renderArray(Object array, Set<Object> seen) {
    var length = Array.getLength(array);
    var limit = Math.min(length, maxCollectionItems);
    var sb = new StringBuilder("[");
    for (var i = 0; i < limit; i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(render(Array.get(array, i), seen));
    }
    if (length > maxCollectionItems) {
      sb.append(", ... (").append(length).append(" total)");
    }
    sb.append("]");
    return sb.toString();
  }

  private String renderRecord(Object record, Set<Object> seen) {
    var components = record.getClass().getRecordComponents();
    var sb = new StringBuilder(record.getClass().getSimpleName()).append("(");
    var limit = Math.min(components.length, maxObjectFields);
    var fields =
        Arrays.stream(components)
            .limit(limit)
            .map(
                comp -> {
                  try {
                    var accessor = comp.getAccessor();
                    accessor.setAccessible(true);
                    var val = accessor.invoke(record);
                    return comp.getName() + ": " + render(val, seen);
                  } catch (Exception e) {
                    return comp.getName() + ": <error>";
                  }
                })
            .collect(Collectors.joining(", "));
    sb.append(fields);
    if (components.length > maxObjectFields) {
      sb.append(", …");
    }
    sb.append(")");
    return sb.toString();
  }

  private String renderObject(Object obj, Set<Object> seen) {
    var clazz = obj.getClass();
    var fields =
        Arrays.stream(clazz.getDeclaredFields())
            .filter(f -> !Modifier.isStatic(f.getModifiers()) && !f.isSynthetic())
            .toArray(Field[]::new);
    var limit = Math.min(fields.length, maxObjectFields);
    var sb = new StringBuilder(clazz.getSimpleName()).append("{");
    for (var i = 0; i < limit; i++) {
      if (i > 0) {
        sb.append(", ");
      }
      var field = fields[i];
      try {
        field.setAccessible(true);
        sb.append(field.getName()).append(": ").append(render(field.get(obj), seen));
      } catch (Exception e) {
        sb.append(field.getName()).append(": <error>");
      }
    }
    if (fields.length > maxObjectFields) {
      sb.append(", ...");
    }
    sb.append("}");
    return sb.toString();
  }

  private static final ClassValue<Boolean> HAS_CUSTOM_TO_STRING =
      new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> clazz) {
          try {
            return clazz.getMethod("toString").getDeclaringClass() != Object.class;
          } catch (NoSuchMethodException e) {
            return false;
          }
        }
      };

  private static final ClassValue<Method> SUMMARY_METHOD_CACHE =
      new ClassValue<>() {
        @Override
        protected Method computeValue(Class<?> clazz) {
          for (var method : clazz.getMethods()) {
            if (method.isAnnotationPresent(NarrativeSummary.class)
                && method.getParameterCount() == 0) {
              return method;
            }
          }
          return null;
        }
      };

  private static Method findNarrativeSummaryMethod(Class<?> clazz) {
    return SUMMARY_METHOD_CACHE.get(clazz);
  }
}
