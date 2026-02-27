package ai.narrativetrace.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NarrativeTraceConfigTest {

  @Test
  void defaultsToDetailLevel() {
    var config = new NarrativeTraceConfig();
    assertThat(config.level()).isEqualTo(TracingLevel.DETAIL);
  }

  @Test
  void acceptsExplicitLevel() {
    var config = new NarrativeTraceConfig(TracingLevel.ERRORS);
    assertThat(config.level()).isEqualTo(TracingLevel.ERRORS);
  }

  @Test
  void levelIsChangeableAtRuntime() {
    var config = new NarrativeTraceConfig();
    assertThat(config.level()).isEqualTo(TracingLevel.DETAIL);

    config.setLevel(TracingLevel.OFF);
    assertThat(config.level()).isEqualTo(TracingLevel.OFF);

    config.setLevel(TracingLevel.ERRORS);
    assertThat(config.level()).isEqualTo(TracingLevel.ERRORS);
  }
}
