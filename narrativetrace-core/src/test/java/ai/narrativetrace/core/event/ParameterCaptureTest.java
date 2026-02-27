package ai.narrativetrace.core.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ParameterCaptureTest {

  @Test
  void capturesNameAndRenderedValueWithRedactedFalse() {
    var capture = new ParameterCapture("customerId", "\"C-123\"", false);

    assertThat(capture.name()).isEqualTo("customerId");
    assertThat(capture.renderedValue()).isEqualTo("\"C-123\"");
    assertThat(capture.redacted()).isFalse();
  }

  @Test
  void supportsRedactedTrue() {
    var capture = new ParameterCapture("password", "[REDACTED]", true);

    assertThat(capture.redacted()).isTrue();
    assertThat(capture.renderedValue()).isEqualTo("[REDACTED]");
  }

  @Test
  void renderedValueReturnsStoredStringForNonRedacted() {
    var capture = new ParameterCapture("customerId", "\"C-123\"", false);

    assertThat(capture.renderedValue()).isEqualTo("\"C-123\"");
  }

  @Test
  void renderedValueReturnsStoredStringForRedacted() {
    var capture = new ParameterCapture("password", "[REDACTED]", true);

    assertThat(capture.renderedValue()).isEqualTo("[REDACTED]");
  }
}
