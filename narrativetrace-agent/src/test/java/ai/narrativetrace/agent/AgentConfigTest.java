package ai.narrativetrace.agent;

import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentConfigTest {

    @Test
    void identifiesTargetClassesByPackageFilter() {
        var config = AgentConfig.parse("packages=ai.narrativetrace.test;com.example");

        assertThat(config.shouldTransform("ai/narrativetrace/test/MyClass")).isTrue();
        assertThat(config.shouldTransform("com/example/Service")).isTrue();
        assertThat(config.shouldTransform("org/other/Class")).isFalse();
    }

    @Test
    void emptyPackagesTransformsNothing() {
        var config = AgentConfig.parse("");

        assertThat(config.shouldTransform("ai/narrativetrace/test/MyClass")).isFalse();
    }

    @Test
    void duplicateKeyThrows() {
        assertThatThrownBy(() -> AgentConfig.parse("packages=ai.narrativetrace.test,packages=com.example"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Duplicate agent config key: packages");
    }

    @Test
    void parsesNullArgs() {
        var config = AgentConfig.parse(null);

        assertThat(config.packages()).isEmpty();
    }

    @Test
    void rejectsPartWithoutEqualsSign() {
        assertThatThrownBy(() -> AgentConfig.parse("packages=ai.narrativetrace.test,malformed"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid agent config entry: malformed");
    }

    @Test
    void emptyPackagesValueDoesNotMatchEverything() {
        var config = AgentConfig.parse("packages=");

        assertThat(config.shouldTransform("com/example/Foo")).isFalse();
    }

    @Test
    void ignoresUnknownKey() {
        var config = AgentConfig.parse("debug=true,packages=ai.narrativetrace.test");

        assertThat(config.shouldTransform("ai/narrativetrace/test/MyClass")).isTrue();
    }

    @Test
    void wildcardPackageMatchesSubpackages() {
        var config = AgentConfig.parse("packages=com.example.*");

        assertThat(config.shouldTransform("com/example/Service")).isTrue();
        assertThat(config.shouldTransform("com/example/app/Service")).isTrue();
        assertThat(config.shouldTransform("com/examplefoo/Service")).isFalse();
        assertThat(config.shouldTransform("org/other/Class")).isFalse();
    }

    @Test
    void doubleWildcardPackageMatchesSubpackages() {
        var config = AgentConfig.parse("packages=com.example.**");

        assertThat(config.shouldTransform("com/example/Service")).isTrue();
        assertThat(config.shouldTransform("com/example/app/deep/Service")).isTrue();
        assertThat(config.shouldTransform("com/examplefoo/Service")).isFalse();
    }

    @Test
    void barePackageEnforcesBoundary() {
        var config = AgentConfig.parse("packages=com.example");

        assertThat(config.shouldTransform("com/example/Service")).isTrue();
        assertThat(config.shouldTransform("com/examplefoo/Service")).isFalse();
    }

    @Test
    void fallsBackToConfigResolverWhenNoCliArgs(@org.junit.jupiter.api.io.TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("narrativetrace.properties"),
                "narrativetrace.packages=com.example.app;com.example.shared\n");
        var classLoader = new URLClassLoader(new URL[]{tempDir.toUri().toURL()}, null);
        var config = AgentConfig.parse(null, classLoader);

        assertThat(config.shouldTransform("com/example/app/Service")).isTrue();
        assertThat(config.shouldTransform("com/example/shared/Util")).isTrue();
        assertThat(config.shouldTransform("org/other/Foo")).isFalse();
    }
}
