package ai.narrativetrace.core.event;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class MethodSignatureTest {

    @Test
    void capturesClassNameMethodNameAndEmptyParams() {
        var signature = new MethodSignature("OrderService", "placeOrder", List.of());

        assertThat(signature.className()).isEqualTo("OrderService");
        assertThat(signature.methodName()).isEqualTo("placeOrder");
        assertThat(signature.parameters()).isEmpty();
    }

    @Test
    void holdsListOfParameters() {
        var params = List.of(
                new ParameterCapture("customerId", "\"C-123\"", false),
                new ParameterCapture("amount", "99.95", false)
        );
        var signature = new MethodSignature("OrderService", "placeOrder", params);

        assertThat(signature.parameters()).hasSize(2);
        assertThat(signature.parameters().get(0).name()).isEqualTo("customerId");
        assertThat(signature.parameters().get(1).name()).isEqualTo("amount");
    }

    @Test
    void holdsOptionalNarrationAndErrorContext() {
        var signature = new MethodSignature("OrderService", "placeOrder", List.of(),
                "Placing order for customer C-123",
                "Context: charging customer C-123");

        assertThat(signature.narration()).isEqualTo("Placing order for customer C-123");
        assertThat(signature.errorContext()).isEqualTo("Context: charging customer C-123");
    }

    @Test
    void narrationAndErrorContextDefaultToNull() {
        var signature = new MethodSignature("OrderService", "placeOrder", List.of());

        assertThat(signature.narration()).isNull();
        assertThat(signature.errorContext()).isNull();
    }
}
