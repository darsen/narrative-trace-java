package ai.narrativetrace.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TracingLevelTest {

  @Test
  void levelsHaveCorrectOrdering() {
    assertThat(TracingLevel.OFF.ordinal()).isLessThan(TracingLevel.ERRORS.ordinal());
    assertThat(TracingLevel.ERRORS.ordinal()).isLessThan(TracingLevel.SUMMARY.ordinal());
    assertThat(TracingLevel.SUMMARY.ordinal()).isLessThan(TracingLevel.NARRATIVE.ordinal());
    assertThat(TracingLevel.NARRATIVE.ordinal()).isLessThan(TracingLevel.DETAIL.ordinal());
  }

  @Test
  void isEnabledReturnsTrueWhenCurrentLevelIsAtOrAboveRequested() {
    assertThat(TracingLevel.DETAIL.isEnabled(TracingLevel.NARRATIVE)).isTrue();
    assertThat(TracingLevel.DETAIL.isEnabled(TracingLevel.DETAIL)).isTrue();
    assertThat(TracingLevel.NARRATIVE.isEnabled(TracingLevel.ERRORS)).isTrue();
    assertThat(TracingLevel.ERRORS.isEnabled(TracingLevel.ERRORS)).isTrue();
  }

  @Test
  void isEnabledReturnsFalseWhenCurrentLevelIsBelowRequested() {
    assertThat(TracingLevel.OFF.isEnabled(TracingLevel.ERRORS)).isFalse();
    assertThat(TracingLevel.ERRORS.isEnabled(TracingLevel.SUMMARY)).isFalse();
    assertThat(TracingLevel.NARRATIVE.isEnabled(TracingLevel.DETAIL)).isFalse();
  }

  @Test
  void offDisablesEverything() {
    assertThat(TracingLevel.OFF.isEnabled(TracingLevel.OFF)).isTrue();
    assertThat(TracingLevel.OFF.isEnabled(TracingLevel.ERRORS)).isFalse();
    assertThat(TracingLevel.OFF.isEnabled(TracingLevel.SUMMARY)).isFalse();
    assertThat(TracingLevel.OFF.isEnabled(TracingLevel.NARRATIVE)).isFalse();
    assertThat(TracingLevel.OFF.isEnabled(TracingLevel.DETAIL)).isFalse();
  }
}
