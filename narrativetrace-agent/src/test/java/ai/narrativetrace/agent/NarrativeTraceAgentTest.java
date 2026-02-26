package ai.narrativetrace.agent;

import org.junit.jupiter.api.Test;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class NarrativeTraceAgentTest {

    @Test
    void premainLoadsSuccessfully() {
        assertThatNoException().isThrownBy(() ->
                NarrativeTraceAgent.premain("packages=ai.narrativetrace.test", null));
    }

    @Test
    void premainWithInstrumentationRegistersTransformer() {
        var registered = new ArrayList<ClassFileTransformer>();
        Instrumentation inst = new StubInstrumentation() {
            @Override
            public void addTransformer(ClassFileTransformer transformer) {
                registered.add(transformer);
            }
        };

        NarrativeTraceAgent.premain("packages=ai.narrativetrace.test", inst);

        assertThat(registered).hasSize(1);
        assertThat(registered.get(0)).isInstanceOf(NarrativeClassFileTransformer.class);
    }
}
