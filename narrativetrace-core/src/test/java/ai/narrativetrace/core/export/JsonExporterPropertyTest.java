package ai.narrativetrace.core.export;

import static org.assertj.core.api.Assertions.assertThat;

import ai.narrativetrace.core.event.MethodSignature;
import ai.narrativetrace.core.event.ParameterCapture;
import ai.narrativetrace.core.event.TraceNode;
import ai.narrativetrace.core.event.TraceOutcome;
import ai.narrativetrace.core.tree.DefaultTraceTree;
import java.util.List;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;

class JsonExporterPropertyTest {

  private final JsonExporter exporter = new JsonExporter();

  @Property
  void exportProducesBalancedBracesAndRequiredFields(
      @ForAll @AlphaChars @StringLength(min = 1, max = 20) String className,
      @ForAll @AlphaChars @StringLength(min = 1, max = 20) String methodName) {
    var sig = new MethodSignature(className, methodName, List.of());
    var node = new TraceNode(sig, List.of(), new TraceOutcome.Returned("ok"));
    var tree = new DefaultTraceTree(List.of(node));

    var json = exporter.export(tree);

    assertThat(countChar(json, '{')).isEqualTo(countChar(json, '}'));
    assertThat(countChar(json, '[')).isEqualTo(countChar(json, ']'));
    assertThat(json).contains("\"class\": \"" + className + "\"");
    assertThat(json).contains("\"method\": \"" + methodName + "\"");
    assertThat(json).contains("\"type\": \"enter\"");
    assertThat(json).contains("\"type\": \"exit\"");
  }

  @Property
  void paramValueEscapingRoundTrips(@ForAll @StringLength(min = 1, max = 30) String value) {
    var param = new ParameterCapture("p", value, false);
    var sig = new MethodSignature("C", "m", List.of(param));
    var node = new TraceNode(sig, List.of(), new TraceOutcome.Returned("ok"));
    var tree = new DefaultTraceTree(List.of(node));

    var json = exporter.export(tree);

    // Extract the escaped value from the JSON and verify round-trip:
    // The param renders as "p": "ESCAPED" — find and unescape it
    var marker = "\"p\": \"";
    int start = json.indexOf(marker);
    assertThat(start).as("param 'p' must appear in JSON output").isGreaterThanOrEqualTo(0);
    var afterMarker = json.substring(start + marker.length());
    var extracted = extractJsonString(afterMarker);
    // Unescape should recover the original value
    var unescaped =
        extracted
            .replace("\\t", "\t")
            .replace("\\r", "\r")
            .replace("\\n", "\n")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\");
    assertThat(unescaped).isEqualTo(value);
  }

  /** Extract a JSON string value up to the next unescaped quote. */
  private static String extractJsonString(String s) {
    var sb = new StringBuilder();
    int i = 0;
    while (i < s.length()) {
      char c = s.charAt(i);
      if (c == '"') {
        return sb.toString();
      }
      sb.append(c);
      if (c == '\\' && i + 1 < s.length()) {
        i++;
        sb.append(s.charAt(i));
      }
      i++;
    }
    return sb.toString();
  }

  private static long countChar(String s, char target) {
    return s.chars().filter(c -> c == target).count();
  }
}
