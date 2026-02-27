package ai.narrativetrace.proxy;

import static org.assertj.core.api.Assertions.assertThat;

import ai.narrativetrace.core.annotation.NotTraced;
import ai.narrativetrace.core.event.ParameterCapture;
import ai.narrativetrace.core.render.ValueRenderer;
import java.util.List;
import org.junit.jupiter.api.Test;

class ParameterNameResolverTest {

  private final ValueRenderer valueRenderer = new ValueRenderer();

  interface Greeter {
    String greet(String name, int times);
  }

  interface AuthService {
    boolean login(String username, @NotTraced String password);
  }

  @Test
  void extractsParamNamesFromInterfaceMethod() throws Exception {
    var method = Greeter.class.getMethod("greet", String.class, int.class);
    var args = new Object[] {"Alice", 3};

    List<ParameterCapture> captures = ParameterNameResolver.resolve(method, args, valueRenderer);

    assertThat(captures).hasSize(2);
    assertThat(captures.get(0).name()).isEqualTo("name");
    assertThat(captures.get(0).renderedValue()).isEqualTo("\"Alice\"");
    assertThat(captures.get(0).redacted()).isFalse();
    assertThat(captures.get(1).name()).isEqualTo("times");
    assertThat(captures.get(1).renderedValue()).isEqualTo("3");
  }

  @Test
  void resolvesFromParamNamesAndRedactedArrays() {
    var paramNames = new String[] {"name", "times"};
    var redacted = new boolean[] {false, false};
    var args = new Object[] {"Alice", 3};

    var captures = ParameterNameResolver.resolve(paramNames, redacted, args, valueRenderer);

    assertThat(captures).hasSize(2);
    assertThat(captures.get(0).name()).isEqualTo("name");
    assertThat(captures.get(0).renderedValue()).isEqualTo("\"Alice\"");
    assertThat(captures.get(0).redacted()).isFalse();
    assertThat(captures.get(1).name()).isEqualTo("times");
    assertThat(captures.get(1).renderedValue()).isEqualTo("3");
  }

  @Test
  void resolvesRedactedFromArrays() {
    var paramNames = new String[] {"username", "password"};
    var redacted = new boolean[] {false, true};
    var args = new Object[] {"admin", "secret"};

    var captures = ParameterNameResolver.resolve(paramNames, redacted, args, valueRenderer);

    assertThat(captures.get(0).redacted()).isFalse();
    assertThat(captures.get(1).redacted()).isTrue();
    assertThat(captures.get(1).renderedValue()).isEqualTo("[REDACTED]");
  }

  @Test
  void detectsNotTracedAnnotationAsRedacted() throws Exception {
    var method = AuthService.class.getMethod("login", String.class, String.class);
    var args = new Object[] {"admin", "secret"};

    var captures = ParameterNameResolver.resolve(method, args, valueRenderer);

    assertThat(captures.get(0).redacted()).isFalse();
    assertThat(captures.get(1).redacted()).isTrue();
    assertThat(captures.get(1).name()).isEqualTo("password");
  }
}
