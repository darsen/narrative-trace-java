package ai.narrativetrace.junit5;

import ai.narrativetrace.core.context.NarrativeContext;
import ai.narrativetrace.core.event.MethodSignature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(NarrativeTraceExtension.class)
class IoErrorFixture {

    @Test
    void testWithTrace(NarrativeContext context) {
        context.enterMethod(new MethodSignature("Service", "doWork", List.of()));
        context.exitMethodWithReturn("ok");
        assertThat(context).isNotNull();
    }
}
