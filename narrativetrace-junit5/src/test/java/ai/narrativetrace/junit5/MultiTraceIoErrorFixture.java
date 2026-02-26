package ai.narrativetrace.junit5;

import ai.narrativetrace.core.context.NarrativeContext;
import ai.narrativetrace.core.event.MethodSignature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
