package ai.narrativetrace.build;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BuildConfigurationTest {

    private static final File PROJECT_DIR = new File(System.getProperty("projectDir"));

    private BuildResult gradle(String... args) {
        var allArgs = new ArrayList<>(List.of(args));
        allArgs.add("-q");
        return GradleRunner.create()
                .withProjectDir(PROJECT_DIR)
                .withArguments(allArgs)
                .build();
    }

    @Test
    void allExpectedModulesAreIncluded() {
        var result = gradle(":projects");
        var output = result.getOutput();

        assertThat(output)
                .contains("narrativetrace-core")
                .contains("narrativetrace-proxy")
                .contains("narrativetrace-junit5")
                .contains("narrativetrace-examples")
                .contains("narrativetrace-clarity")
                .contains("narrativetrace-diagrams")
                .contains("narrativetrace-slf4j")
                .contains("narrativetrace-agent")
                .contains("narrativetrace-spring")
                .contains("narrativetrace-benchmarks")
                .contains("narrativetrace-micrometer")
                .contains("narrativetrace-build-tests")
                .contains("narrativetrace-gradle-plugin");
    }

    @Test
    void agentJarManifest() throws Exception {
        gradle(":narrativetrace-agent:jar");
        var jarDir = new File(PROJECT_DIR, "narrativetrace-agent/build/libs");
        var jarFiles = jarDir.listFiles((dir, name) -> name.endsWith(".jar"));
        assertThat(jarFiles).isNotEmpty();

        try (var jar = new java.util.jar.JarFile(jarFiles[0])) {
            var manifest = jar.getManifest();
            var attrs = manifest.getMainAttributes();
            assertThat(attrs.getValue("Premain-Class"))
                    .isEqualTo("ai.narrativetrace.agent.NarrativeTraceAgent");
            assertThat(attrs.getValue("Can-Retransform-Classes"))
                    .isEqualTo("true");
        }
    }

    @Test
    void coreModulePublicationConfigured() {
        var result = gradle(":narrativetrace-core:tasks", "--all");
        assertThat(result.getOutput()).contains("publishMavenJavaPublicationToMavenLocal");
    }

    @Test
    void exampleCustomTasksRegistered() {
        var result = gradle(":narrativetrace-examples:tasks", "--all");
        var output = result.getOutput();

        assertThat(output)
                .contains("runEcommerce")
                .contains("runMinecraft")
                .contains("runLibrary")
                .contains("runClarity")
                .contains("traceTests")
                .contains("traceMermaid")
                .contains("tracePlantUml")
                .contains("renderDiagrams");
    }

    @Test
    void metricsReportTaskExistsAtRoot() {
        var result = gradle("tasks", "--group=verification");
        assertThat(result.getOutput()).contains("metricsReport");
    }

    @Test
    void pmdTasksExistOnProductionModules() {
        var result = gradle(":narrativetrace-core:tasks", "--all");
        var output = result.getOutput();

        assertThat(output).contains("pmdMain").contains("pmdMetrics");
    }

    @Test
    void testFinalizedByJacocoReport() {
        var result = gradle(":narrativetrace-core:test", "--dry-run");
        assertThat(result.getOutput()).contains("jacocoTestReport");
    }

    @Test
    void jacocoVerificationNotWiredForBenchmarks() {
        var result = gradle(":narrativetrace-benchmarks:check", "--dry-run");
        assertThat(result.getOutput()).doesNotContain("jacocoTestCoverageVerification");
    }

    @Test
    void jacocoVerificationWiredIntoCheckForProductionModules() {
        var result = gradle(":narrativetrace-core:check", "--dry-run");
        assertThat(result.getOutput()).contains("jacocoTestCoverageVerification");
    }

    @Test
    void springDependsOnCoreProxy() {
        var result = gradle(":narrativetrace-spring:dependencies", "--configuration", "compileClasspath");
        var output = result.getOutput();

        assertThat(output)
                .contains("narrativetrace-core")
                .contains("narrativetrace-proxy");
    }

    @Test
    void examplesDependsOnSpringMicrometerSlf4j() {
        var result = gradle(":narrativetrace-examples:dependencies", "--configuration", "compileClasspath");
        var output = result.getOutput();

        assertThat(output)
                .contains("narrativetrace-spring")
                .contains("narrativetrace-micrometer")
                .contains("narrativetrace-slf4j");
    }

    @Test
    void junit5DependsOnCoreProxyDiagramsClarity() {
        var result = gradle(":narrativetrace-junit5:dependencies", "--configuration", "compileClasspath");
        var output = result.getOutput();

        assertThat(output)
                .contains("narrativetrace-core")
                .contains("narrativetrace-proxy")
                .contains("narrativetrace-diagrams")
                .contains("narrativetrace-clarity");
    }

    @Test
    void proxyDependsOnCore() {
        var result = gradle(":narrativetrace-proxy:dependencies", "--configuration", "compileClasspath");
        assertThat(result.getOutput()).contains("narrativetrace-core");
    }

    @Test
    void fullCheckDryRunResolvesWithoutErrors() {
        var result = gradle("check", "--dry-run");
        var output = result.getOutput();

        assertThat(output)
                .contains(":narrativetrace-core:check")
                .contains(":narrativetrace-proxy:check")
                .contains(":narrativetrace-junit5:check")
                .contains(":narrativetrace-clarity:check")
                .contains(":narrativetrace-diagrams:check")
                .contains(":narrativetrace-slf4j:check")
                .contains(":narrativetrace-agent:check")
                .contains(":narrativetrace-spring:check")
                .contains(":narrativetrace-micrometer:check")
                .contains(":narrativetrace-examples:check")
                .contains(":narrativetrace-gradle-plugin:check");
    }

    @Test
    void parametersCompilerFlagPresent() {
        var result = gradle(":narrativetrace-core:printCompilerArgs");
        var output = result.getOutput();

        assertThat(output).contains("COMPILER_ARGS: [-parameters]");
    }

    @Test
    void java17SourceCompatibility() {
        var result = gradle(":narrativetrace-core:properties");
        var output = result.getOutput();

        assertThat(output).contains("sourceCompatibility: 17");
    }
}
