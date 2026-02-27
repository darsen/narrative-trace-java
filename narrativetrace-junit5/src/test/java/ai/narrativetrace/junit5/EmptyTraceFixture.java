package ai.narrativetrace.junit5;

import static org.assertj.core.api.Assertions.assertThat;

import ai.narrativetrace.core.context.NarrativeContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(NarrativeTraceExtension.class)
class EmptyTraceFixture {

  @Test
  void testWithEmptyTrace(NarrativeContext context) {
    // Don't enter any methods — trace stays empty
    assertThat(context).isNotNull();
  }
}
