package ai.narrativetrace.agent;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NarrativeClassFileTransformerTest {

  @Test
  void returnsNullForNullClassName() {
    var config = AgentConfig.parse("packages=ai.narrativetrace.test");
    var transformer = new NarrativeClassFileTransformer(config);

    var result = transformer.transform(null, null, null, null, new byte[0]);

    assertThat(result).isNull();
  }

  @Test
  void returnsNullForClassOutsideConfiguredPackages() {
    var config = AgentConfig.parse("packages=ai.narrativetrace.test");
    var transformer = new NarrativeClassFileTransformer(config);

    var result = transformer.transform(null, "com/other/SomeClass", null, null, new byte[0]);

    assertThat(result).isNull();
  }

  @Test
  void returnsTransformedBytesForClassInsideConfiguredPackages() throws Exception {
    var config = AgentConfig.parse("packages=ai.narrativetrace.agent.sample");
    var transformer = new NarrativeClassFileTransformer(config);
    var originalBytes =
        getClass()
            .getClassLoader()
            .getResourceAsStream("ai/narrativetrace/agent/sample/Calculator.class")
            .readAllBytes();

    var result =
        transformer.transform(
            null, "ai/narrativetrace/agent/sample/Calculator", null, null, originalBytes);

    assertThat(result).isNotNull();
    assertThat(result).isNotEqualTo(originalBytes);
  }
}
