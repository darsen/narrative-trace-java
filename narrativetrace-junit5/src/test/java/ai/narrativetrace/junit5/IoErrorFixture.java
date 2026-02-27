package ai.narrativetrace.junit5;

import static org.assertj.core.api.Assertions.assertThat;

import ai.narrativetrace.core.context.NarrativeContext;
import ai.narrativetrace.core.event.MethodSignature;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(NarrativeTraceExtension.class)
class IoErrorFixture {

  @Test
  void testWithTrace(NarrativeContext context) {
    context.enterMethod(new MethodSignature("Service", "doWork", List.of()));
    context.exitMethodWithReturn("ok");
    assertThat(context).isNotNull();
  }
}
