package ai.narrativetrace.proxy;

import static org.assertj.core.api.Assertions.assertThat;

import ai.narrativetrace.core.annotation.OnError;
import org.junit.jupiter.api.Test;

class OnErrorTest {

  interface MultipleOnErrorService {
    @OnError(value = "General failure", exception = RuntimeException.class)
    @OnError(value = "Payment declined", exception = IllegalStateException.class)
    void charge(double amount);
  }

  @Test
  void repeatableAnnotationAllowsMultipleOnErrorPerMethod() throws NoSuchMethodException {
    var method = MultipleOnErrorService.class.getMethod("charge", double.class);
    var annotations = method.getAnnotationsByType(OnError.class);

    assertThat(annotations).hasSize(2);
    assertThat(annotations[0].value()).isEqualTo("General failure");
    assertThat(annotations[0].exception()).isEqualTo(RuntimeException.class);
    assertThat(annotations[1].value()).isEqualTo("Payment declined");
    assertThat(annotations[1].exception()).isEqualTo(IllegalStateException.class);
  }
}
