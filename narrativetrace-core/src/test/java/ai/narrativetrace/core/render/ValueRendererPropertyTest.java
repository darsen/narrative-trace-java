package ai.narrativetrace.core.render;

import static org.assertj.core.api.Assertions.assertThat;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;

class ValueRendererPropertyTest {

  private final ValueRenderer renderer = new ValueRenderer();

  @Property
  void renderNeverReturnsNull(
      @ForAll @IntRange(min = Integer.MIN_VALUE, max = Integer.MAX_VALUE) int value) {
    assertThat(renderer.render(value)).isNotNull();
  }

  @Property
  void stringRenderingAlwaysWrapsInQuotes(@ForAll @StringLength(max = 100) String input) {
    var result = renderer.render(input);
    assertThat(result).startsWith("\"");
    assertThat(result).endsWith("\"");
  }

  @Property
  void longStringsAreTruncated(@ForAll @StringLength(min = 201, max = 500) String input) {
    var result = renderer.render(input);
    assertThat(result).contains("\u2026\"");
    // Truncated output should be shorter than rendering the full string
    assertThat(result.length()).isLessThan(input.length() + 3);
  }
}
