package ai.narrativetrace.clarity;

import java.util.Set;

/** Detects generic or meaningless tokens in identifiers (e.g., data, info, temp, obj). */
public final class GenericTokenDetector {

  public enum Tier {
    MEANINGLESS,
    VAGUE,
    TYPED_GENERIC,
    NOT_GENERIC
  }

  public record Result(Tier tier, double score) {}

  private static final Set<String> MEANINGLESS_PLACEHOLDERS =
      Set.of(
          "foo", "bar", "baz", "qux", "quux", "temp", "tmp", "test", "dummy", "sample", "example",
          "xxx", "yyy", "zzz");

  private static final Set<String> VAGUE_WORDS =
      Set.of(
          "data",
          "info",
          "object",
          "thing",
          "item",
          "element",
          "stuff",
          "result",
          "response",
          "output",
          "input",
          "value",
          "content",
          "payload",
          "resource",
          "record",
          "entry",
          "detail",
          "details",
          "entity",
          "bean",
          "model",
          "wrapper",
          "holder",
          "container",
          "bundle",
          "batch",
          "chunk",
          "block",
          "piece",
          "part",
          "unit",
          "instance",
          "param",
          "argument",
          "body",
          "obj",
          "val",
          "arg");

  private static final Set<String> TYPED_GENERIC_WORDS =
      Set.of(
          "id",
          "name",
          "type",
          "status",
          "state",
          "count",
          "size",
          "length",
          "index",
          "key",
          "flag",
          "code",
          "text",
          "message",
          "label",
          "number",
          "amount",
          "total",
          "level",
          "mode",
          "kind",
          "category",
          "group",
          "list",
          "map",
          "set",
          "queue",
          "stack",
          "array",
          "collection",
          "table",
          "row",
          "column",
          "field",
          "property",
          "tag",
          "version",
          "timestamp",
          "date",
          "time",
          "duration",
          "interval",
          "timeout",
          "limit",
          "offset",
          "page",
          "sort",
          "order",
          "direction",
          "position",
          "priority",
          "weight",
          "rank",
          "score",
          "rating",
          "percentage",
          "ratio",
          "factor",
          "coefficient");

  public Result detect(String token) {
    var lower = token.toLowerCase();

    if (isMeaninglessSingleLetter(lower) || MEANINGLESS_PLACEHOLDERS.contains(lower)) {
      return new Result(Tier.MEANINGLESS, 0.0);
    }
    if (VAGUE_WORDS.contains(lower)) {
      return new Result(Tier.VAGUE, 0.2);
    }
    if (TYPED_GENERIC_WORDS.contains(lower)) {
      return new Result(Tier.TYPED_GENERIC, 0.5);
    }
    return new Result(Tier.NOT_GENERIC, 1.0);
  }

  private boolean isMeaninglessSingleLetter(String lower) {
    if (lower.length() != 1) return false;
    char c = lower.charAt(0);
    return c >= 'a' && c <= 'z';
  }
}
