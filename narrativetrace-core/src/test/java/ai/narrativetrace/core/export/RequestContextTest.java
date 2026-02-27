package ai.narrativetrace.core.export;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RequestContextTest {

  @Test
  void constructAndVerifyAccessors() {
    var ctx = new RequestContext("POST", "/api/orders", 201, 42L);

    assertThat(ctx.method()).isEqualTo("POST");
    assertThat(ctx.uri()).isEqualTo("/api/orders");
    assertThat(ctx.statusCode()).isEqualTo(201);
    assertThat(ctx.durationMillis()).isEqualTo(42L);
  }
}
