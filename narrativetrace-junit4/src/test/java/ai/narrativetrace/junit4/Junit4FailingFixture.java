package ai.narrativetrace.junit4;

import ai.narrativetrace.core.event.MethodSignature;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.fail;

public class Junit4FailingFixture {

    @Rule
    public NarrativeTraceRule narrativeTrace = new NarrativeTraceRule();

    @Test
    public void failingTest() {
        narrativeTrace.context().enterMethod(new MethodSignature("Service", "doWork", List.of()));
        narrativeTrace.context().exitMethodWithReturn("ok");
        fail("deliberate failure");
    }
}
