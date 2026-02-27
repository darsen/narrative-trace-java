package ai.narrativetrace.examples.ecommerce;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ExternalServiceExceptionTest {

  @Test
  void twoArgConstructorPreservesMessageAndCause() {
    var cause = new RuntimeException("network error");
    var exception = new ExternalServiceException("failed", cause);

    assertThat(exception.getMessage()).isEqualTo("failed");
    assertThat(exception.getCause()).isSameAs(cause);
  }
}
