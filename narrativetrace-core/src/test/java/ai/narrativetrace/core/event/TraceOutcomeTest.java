package ai.narrativetrace.core.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TraceOutcomeTest {

  @Test
  void returnedHoldsReturnValue() {
    var outcome = new TraceOutcome.Returned("\"order-42\"");

    assertThat(outcome.renderedValue()).isEqualTo("\"order-42\"");
    assertThat(outcome).isInstanceOf(TraceOutcome.class);
  }

  @Test
  void threwHoldsException() {
    var exception = new RuntimeException("insufficient funds");
    var outcome = new TraceOutcome.Threw(exception);

    assertThat(outcome.exception()).isSameAs(exception);
    assertThat(outcome).isInstanceOf(TraceOutcome.class);
  }
}
