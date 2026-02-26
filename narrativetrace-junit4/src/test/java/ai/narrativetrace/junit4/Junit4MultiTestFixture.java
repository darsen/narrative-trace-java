package ai.narrativetrace.junit4;

import ai.narrativetrace.core.event.MethodSignature;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

public class Junit4MultiTestFixture {

    @ClassRule
    public static NarrativeTraceClassRule classRule = new NarrativeTraceClassRule();

    @Rule
    public NarrativeTraceRule narrativeTrace = classRule.testRule();

    @Test
    public void customerPlacesOrder() {
        narrativeTrace.context().enterMethod(new MethodSignature("OrderService", "placeOrder", List.of()));
        narrativeTrace.context().exitMethodWithReturn("order-1");
    }

    @Test
    public void customerCancelsOrder() {
        narrativeTrace.context().enterMethod(new MethodSignature("OrderService", "cancelOrder", List.of()));
        narrativeTrace.context().exitMethodWithReturn("cancelled");
    }
}
