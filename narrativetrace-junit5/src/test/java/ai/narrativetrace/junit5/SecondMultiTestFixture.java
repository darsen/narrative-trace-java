package ai.narrativetrace.junit5;

import ai.narrativetrace.core.context.NarrativeContext;
import ai.narrativetrace.core.event.MethodSignature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

@ExtendWith(NarrativeTraceExtension.class)
class SecondMultiTestFixture {

    @Test
    @DisplayName("customer checks inventory")
    void customerChecksInventory(NarrativeContext context) {
        context.enterMethod(new MethodSignature("InventoryService", "checkStock", List.of()));
        context.exitMethodWithReturn("in-stock");
    }
}
