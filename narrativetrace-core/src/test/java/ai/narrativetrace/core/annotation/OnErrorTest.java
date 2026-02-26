package ai.narrativetrace.core.annotation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OnErrorTest {

    @OnError("Context: charging customer {customerId}, amount was {amount}")
    void exampleMethod() {
    }

    @Test
    void annotationHasValueWithTemplate() throws NoSuchMethodException {
        var method = OnErrorTest.class.getDeclaredMethod("exampleMethod");
        var annotation = method.getAnnotation(OnError.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("Context: charging customer {customerId}, amount was {amount}");
    }
}
