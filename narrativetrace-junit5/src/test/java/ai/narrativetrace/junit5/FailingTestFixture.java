package ai.narrativetrace.junit5;

import ai.narrativetrace.core.context.NarrativeContext;
import ai.narrativetrace.core.event.MethodSignature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(NarrativeTraceExtension.class)
class FailingTestFixture {

    @Test
    @DisplayName("failing test")
    void failingTest(NarrativeContext context) {
        context.enterMethod(new MethodSignature("Service", "doWork", List.of()));
        context.exitMethodWithReturn("ok");
        assertThat(true).isFalse();
    }
}
