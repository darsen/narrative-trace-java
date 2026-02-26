package ai.narrativetrace.core.annotation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NarratedTest {

    @Narrated("Placing order of {quantity} units for customer {customerId}")
    void exampleMethod() {
    }

    @Test
    void annotationHasValueWithTemplate() throws NoSuchMethodException {
        var method = NarratedTest.class.getDeclaredMethod("exampleMethod");
        var annotation = method.getAnnotation(Narrated.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("Placing order of {quantity} units for customer {customerId}");
    }
}
