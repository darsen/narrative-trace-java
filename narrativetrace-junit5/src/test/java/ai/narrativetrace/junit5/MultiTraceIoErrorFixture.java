package ai.narrativetrace.junit5;

import static org.assertj.core.api.Assertions.assertThat;

import ai.narrativetrace.core.context.NarrativeContext;
import ai.narrativetrace.core.event.MethodSignature;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(NarrativeTraceExtension.class)
class MultiTraceIoErrorFixture {

  @Test
  void firstTest(NarrativeContext context) {
    context.enterMethod(new MethodSignature("Service", "first", List.of()));
    context.exitMethodWithReturn("ok");
    assertThat(context).isNotNull();
  }

  @Test
  void secondTest(NarrativeContext context) {
    context.enterMethod(new MethodSignature("Service", "second", List.of()));
    context.exitMethodWithReturn("ok");
    assertThat(context).isNotNull();
  }
}
