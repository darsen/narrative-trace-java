package ai.narrativetrace.junit5;

import ai.narrativetrace.core.context.NarrativeContext;
import ai.narrativetrace.core.event.MethodSignature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

@ExtendWith(NarrativeTraceExtension.class)
class MultiTestFixture {

    @Test
    @DisplayName("customer places order")
    void customerPlacesOrder(NarrativeContext context) {
        context.enterMethod(new MethodSignature("OrderService", "placeOrder", List.of()));
        context.exitMethodWithReturn("order-1");
    }

    @Test
    @DisplayName("customer cancels order")
    void customerCancelsOrder(NarrativeContext context) {
        context.enterMethod(new MethodSignature("OrderService", "cancelOrder", List.of()));
        context.exitMethodWithReturn("cancelled");
    }
}
