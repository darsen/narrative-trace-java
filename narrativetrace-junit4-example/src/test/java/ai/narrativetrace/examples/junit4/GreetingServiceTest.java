package ai.narrativetrace.examples.junit4;

import ai.narrativetrace.junit4.NarrativeTraceClassRule;
import ai.narrativetrace.junit4.NarrativeTraceRule;
import ai.narrativetrace.proxy.NarrativeTraceProxy;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GreetingServiceTest {

    @ClassRule
    public static NarrativeTraceClassRule classRule = new NarrativeTraceClassRule();

    @Rule
    public NarrativeTraceRule narrativeTrace = classRule.testRule();

    @Test
    public void greetsByName() {
        var service = NarrativeTraceProxy.trace(
                new DefaultGreetingService(), GreetingService.class, narrativeTrace.context());
        assertEquals("greeting message", "Hello, Alice!", service.greet("Alice"));
    }
}
