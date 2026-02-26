package ai.narrativetrace.gradle;

import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;

class NarrativeTracePluginFunctionalTest {

    @Test
    void pluginCanBeApplied(@TempDir Path projectDir) throws IOException {
        writeBuildFile(projectDir, "");

        var result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("tasks")
                .build();

        assertThat(result.task(":tasks").getOutcome()).isEqualTo(SUCCESS);
    }

    @Test
    void extensionIsRegistered(@TempDir Path projectDir) throws IOException {
        writeBuildFile(projectDir,
                "tasks.register(\"checkExtension\") {\n"
                + "    doLast {\n"
                + "        val ext = project.extensions.getByName(\"narrativeTrace\")\n"
                + "        println(\"EXTENSION_FOUND: \" + ext.javaClass.simpleName)\n"
                + "    }\n"
                + "}\n");

        var result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("checkExtension")
                .build();

        assertThat(result.getOutput()).contains("EXTENSION_FOUND:");
    }

    @Test
    void claritySubExtensionAccessible(@TempDir Path projectDir) throws IOException {
        writeBuildFile(projectDir,
                "narrativeTrace {\n"
                + "    clarity {\n"
                + "        minScore.set(0.80)\n"
                + "        maxHighIssues.set(0)\n"
                + "    }\n"
                + "}\n"
                + "tasks.register(\"checkClarity\") {\n"
                + "    doLast {\n"
                + "        val ext = project.extensions.getByType(ai.narrativetrace.gradle.NarrativeTraceExtension::class.java)\n"
                + "        println(\"MIN_SCORE: \" + ext.clarity.minScore.get())\n"
                + "        println(\"MAX_HIGH: \" + ext.clarity.maxHighIssues.get())\n"
                + "    }\n"
                + "}\n");

        var result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("checkClarity")
                .build();

        assertThat(result.getOutput()).contains("MIN_SCORE: 0.8");
        assertThat(result.getOutput()).contains("MAX_HIGH: 0");
    }

    @Test
    void pluginAddsParametersCompilerFlag(@TempDir Path projectDir) throws IOException {
        writeBuildFile(projectDir,
                "tasks.withType<JavaCompile> {\n"
                + "    doFirst {\n"
                + "        println(\"COMPILER_ARGS: \" + options.compilerArgs)\n"
                + "    }\n"
                + "}\n");
        writeJavaSource(projectDir);

        var result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("compileJava")
                .build();

        assertThat(result.getOutput()).contains("-parameters");
    }

    @Test
    void pluginDoesNotDuplicateParametersFlag(@TempDir Path projectDir) throws IOException {
        writeBuildFile(projectDir,
                "tasks.withType<JavaCompile> {\n"
                + "    options.compilerArgs.add(\"-parameters\")\n"
                + "    doFirst {\n"
                + "        val count = options.compilerArgs.count { it == \"-parameters\" }\n"
                + "        println(\"PARAMS_COUNT: \" + count)\n"
                + "    }\n"
                + "}\n");
        writeJavaSource(projectDir);

        var result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("compileJava")
                .build();

        assertThat(result.getOutput()).contains("PARAMS_COUNT: 1");
    }

    @Test
    void pluginAddsDependencies(@TempDir Path projectDir) throws IOException {
        writeBuildFile(projectDir,
                "tasks.register(\"listDeps\") {\n"
                + "    doLast {\n"
                + "        configurations.getByName(\"testImplementation\").dependencies.forEach {\n"
                + "            println(\"DEP: \" + it.group + \":\" + it.name)\n"
                + "        }\n"
                + "    }\n"
                + "}\n");

        var result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("listDeps")
                .build();

        assertThat(result.getOutput())
                .contains("DEP: ai.narrativetrace:narrativetrace-core")
                .contains("DEP: ai.narrativetrace:narrativetrace-proxy")
                .contains("DEP: ai.narrativetrace:narrativetrace-clarity")
                .contains("DEP: ai.narrativetrace:narrativetrace-diagrams")
                .contains("DEP: ai.narrativetrace:narrativetrace-junit5");
    }

    @Test
    void pluginAddsJunit4WhenConfigured(@TempDir Path projectDir) throws IOException {
        writeBuildFile(projectDir,
                "narrativeTrace {\n"
                + "    testFramework.set(\"junit4\")\n"
                + "}\n"
                + "tasks.register(\"listDeps\") {\n"
                + "    doLast {\n"
                + "        configurations.getByName(\"testImplementation\").dependencies.forEach {\n"
                + "            println(\"DEP: \" + it.group + \":\" + it.name)\n"
                + "        }\n"
                + "    }\n"
                + "}\n");

        var result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("listDeps")
                .build();

        assertThat(result.getOutput())
                .contains("DEP: ai.narrativetrace:narrativetrace-junit4")
                .doesNotContain("DEP: ai.narrativetrace:narrativetrace-junit5");
    }

    @Test
    void pluginSkipsDependenciesWhenManageDependenciesFalse(@TempDir Path projectDir) throws IOException {
        writeBuildFile(projectDir,
                "narrativeTrace {\n"
                + "    manageDependencies.set(false)\n"
                + "}\n"
                + "tasks.register(\"listDeps\") {\n"
                + "    doLast {\n"
                + "        configurations.getByName(\"testImplementation\").dependencies.forEach {\n"
                + "            println(\"DEP: \" + it.group + \":\" + it.name)\n"
                + "        }\n"
                + "    }\n"
                + "}\n");

        var result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("listDeps")
                .build();

        assertThat(result.getOutput()).doesNotContain("DEP: ai.narrativetrace:");
    }

    @Test
    void pluginSetsOutputSystemProperties(@TempDir Path projectDir) throws IOException {
        writeBuildFile(projectDir,
                "tasks.register(\"checkTestConfig\") {\n"
                + "    doLast {\n"
                + "        val testTask = tasks.named(\"test\", Test::class.java).get()\n"
                + "        println(\"SYS_OUTPUT: \" + testTask.systemProperties[\"narrativetrace.output\"])\n"
                + "        println(\"SYS_DIR: \" + testTask.systemProperties[\"narrativetrace.outputDir\"])\n"
                + "    }\n"
                + "}\n");

        var result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("checkTestConfig")
                .build();

        assertThat(result.getOutput()).contains("SYS_OUTPUT: true");
        assertThat(result.getOutput()).contains("SYS_DIR:");
        assertThat(result.getOutput()).contains("narrativetrace");
    }

    @Test
    void customOutputDirIsForwarded(@TempDir Path projectDir) throws IOException {
        writeBuildFile(projectDir,
                "narrativeTrace {\n"
                + "    outputDir.set(layout.buildDirectory.dir(\"custom-traces\"))\n"
                + "}\n"
                + "tasks.register(\"checkTestConfig\") {\n"
                + "    doLast {\n"
                + "        val testTask = tasks.named(\"test\", Test::class.java).get()\n"
                + "        println(\"SYS_DIR: \" + testTask.systemProperties[\"narrativetrace.outputDir\"])\n"
                + "    }\n"
                + "}\n");

        var result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("checkTestConfig")
                .build();

        assertThat(result.getOutput()).contains("custom-traces");
    }

    @Test
    void disabledPluginDoesNotConfigureTests(@TempDir Path projectDir) throws IOException {
        writeBuildFile(projectDir,
                "narrativeTrace {\n"
                + "    enabled.set(false)\n"
                + "}\n"
                + "tasks.register(\"checkTestConfig\") {\n"
                + "    doLast {\n"
                + "        val testTask = tasks.named(\"test\", Test::class.java).get()\n"
                + "        println(\"SYS_OUTPUT: \" + testTask.systemProperties[\"narrativetrace.output\"])\n"
                + "    }\n"
                + "}\n");

        var result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("checkTestConfig")
                .build();

        assertThat(result.getOutput()).contains("SYS_OUTPUT: null");
    }

    @Test
    void clarityCheckTaskIsRegistered(@TempDir Path projectDir) throws IOException {
        writeBuildFile(projectDir, "");

        var result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("tasks", "--all")
                .build();

        assertThat(result.getOutput()).contains("clarityCheck");
    }

    @Test
    void clarityCheckDependsOnTest(@TempDir Path projectDir) throws IOException {
        writeBuildFile(projectDir, "");

        var result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("clarityCheck", "--dry-run")
                .build();

        var output = result.getOutput();
        int testIndex = output.indexOf(":test ");
        int clarityIndex = output.indexOf(":clarityCheck ");
        assertThat(testIndex).isGreaterThanOrEqualTo(0);
        assertThat(clarityIndex).isGreaterThan(testIndex);
    }

    @Test
    void checkDependsOnClarityCheck(@TempDir Path projectDir) throws IOException {
        writeBuildFile(projectDir, "");

        var result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("check", "--dry-run")
                .build();

        assertThat(result.getOutput()).contains(":clarityCheck ");
    }

    @Test
    void clarityCheckPassesWhenNoThreshold(@TempDir Path projectDir) throws IOException {
        writeBuildFile(projectDir, "");
        writeClarityJson(projectDir, scenarioJson("Test", 0.50));

        var result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("clarityCheck", "-x", "test")
                .build();

        assertThat(result.task(":clarityCheck").getOutcome()).isEqualTo(SUCCESS);
    }

    @Test
    void clarityCheckPassesWhenScoreMeetsThreshold(@TempDir Path projectDir) throws IOException {
        writeBuildFile(projectDir,
                "narrativeTrace { clarity { minScore.set(0.80) } }\n");
        writeClarityJson(projectDir, scenarioJson("Test", 0.85));

        var result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("clarityCheck", "-x", "test")
                .build();

        assertThat(result.task(":clarityCheck").getOutcome()).isEqualTo(SUCCESS);
    }

    @Test
    void clarityCheckFailsWhenScoreBelowThreshold(@TempDir Path projectDir) throws IOException {
        writeBuildFile(projectDir,
                "narrativeTrace { clarity { minScore.set(0.80) } }\n");
        writeClarityJson(projectDir, scenarioJson("Checkout flow", 0.60));

        var result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("clarityCheck", "-x", "test")
                .buildAndFail();

        assertThat(result.getOutput()).contains("Clarity check failed");
        assertThat(result.getOutput()).contains("Checkout flow");
        assertThat(result.getOutput()).contains("0.60");
    }

    @Test
    void clarityCheckReportsAllFailingScenarios(@TempDir Path projectDir) throws IOException {
        writeBuildFile(projectDir,
                "narrativeTrace { clarity { minScore.set(0.80) } }\n");
        writeClarityJson(projectDir,
                scenarioJson("First", 0.50) + "," + scenarioJson("Second", 0.60));

        var result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("clarityCheck", "-x", "test")
                .buildAndFail();

        assertThat(result.getOutput()).contains("First").contains("Second");
    }

    @Test
    void clarityCheckEnforcesMaxHighIssues(@TempDir Path projectDir) throws IOException {
        writeBuildFile(projectDir,
                "narrativeTrace { clarity { maxHighIssues.set(0) } }\n");
        var json = "{\"version\":\"1.0\",\"scenarios\":["
                + "{\"name\":\"Test\",\"overallScore\":0.90"
                + ",\"methodNameScore\":0.90,\"classNameScore\":0.90"
                + ",\"parameterNameScore\":0.90,\"structuralScore\":0.90,\"cohesionScore\":0.90"
                + ",\"issues\":[{\"category\":\"param-name\",\"element\":\"x\""
                + ",\"suggestion\":\"fix\",\"severity\":\"HIGH\",\"occurrences\":3,\"impactScore\":9.00}]"
                + "}]}";
        writeClarityJsonRaw(projectDir, json);

        var result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("clarityCheck", "-x", "test")
                .buildAndFail();

        assertThat(result.getOutput()).contains("HIGH issues");
    }

    @Test
    void clarityCheckWarnsWhenWarnOnly(@TempDir Path projectDir) throws IOException {
        writeBuildFile(projectDir,
                "narrativeTrace { clarity { minScore.set(0.80)\nwarnOnly.set(true) } }\n");
        writeClarityJson(projectDir, scenarioJson("Test", 0.50));

        var result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("clarityCheck", "-x", "test")
                .build();

        assertThat(result.task(":clarityCheck").getOutcome()).isEqualTo(SUCCESS);
    }

    @Test
    void clarityCheckSkipsWhenJsonMissing(@TempDir Path projectDir) throws IOException {
        writeBuildFile(projectDir,
                "narrativeTrace { clarity { minScore.set(0.80) } }\n");

        var result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("clarityCheck", "-x", "test")
                .build();

        assertThat(result.getOutput()).contains("No clarity-results.json found");
    }

    @Test
    void clarityCheckFailsOnMalformedJson(@TempDir Path projectDir) throws IOException {
        writeBuildFile(projectDir, "");
        writeClarityJsonRaw(projectDir, "{not valid}");

        var result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("clarityCheck", "-x", "test")
                .buildAndFail();

        assertThat(result.getOutput()).contains("Failed to parse clarity results");
    }

    @Test
    void disabledPluginRegistersNoTasks(@TempDir Path projectDir) throws IOException {
        writeBuildFile(projectDir,
                "narrativeTrace {\n"
                + "    enabled.set(false)\n"
                + "}\n");

        var result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("tasks", "--all")
                .build();

        assertThat(result.getOutput()).doesNotContain("clarityCheck");
    }

    @Test
    void pluginWorksInSubproject(@TempDir Path projectDir) throws IOException {
        Files.writeString(projectDir.resolve("settings.gradle.kts"),
                "rootProject.name = \"multi-project\"\ninclude(\"sub\")");
        Files.writeString(projectDir.resolve("build.gradle.kts"), "");
        var subDir = projectDir.resolve("sub");
        Files.createDirectories(subDir);
        Files.writeString(subDir.resolve("build.gradle.kts"),
                "plugins {\n"
                + "    java\n"
                + "    id(\"ai.narrativetrace\")\n"
                + "}\n"
                + "repositories { mavenCentral() }\n");

        var result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments(":sub:tasks", "--all")
                .build();

        assertThat(result.getOutput()).contains("clarityCheck");
    }

    @Test
    void clarityScanTaskIsRegistered(@TempDir Path projectDir) throws IOException {
        writeBuildFile(projectDir, "");

        var result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("tasks", "--all")
                .build();

        assertThat(result.getOutput()).contains("clarityScan");
    }

    @Test
    void clarityScanTaskDependsOnClasses(@TempDir Path projectDir) throws IOException {
        writeBuildFile(projectDir, "");
        writeJavaSource(projectDir);

        var result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("clarityScan", "--dry-run")
                .build();

        assertThat(result.getOutput()).contains(":classes");
        assertThat(result.getOutput()).contains(":clarityScan");
    }

    @Test
    void clarityScanTaskHasCorrectConfiguration(@TempDir Path projectDir) throws IOException {
        writeBuildFile(projectDir,
                "afterEvaluate {\n"
                + "    tasks.named<JavaExec>(\"clarityScan\") {\n"
                + "        doFirst {\n"
                + "            println(\"MAIN_CLASS: \" + mainClass.get())\n"
                + "            println(\"ARGS: \" + args)\n"
                + "        }\n"
                + "    }\n"
                + "}\n");

        var result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("clarityScan", "--dry-run")
                .build();

        assertThat(result.getOutput()).contains(":clarityScan");
    }

    private String scenarioJson(String name, double score) {
        return String.format("{\"name\":\"%s\",\"overallScore\":%.2f"
                + ",\"methodNameScore\":%.2f,\"classNameScore\":%.2f"
                + ",\"parameterNameScore\":%.2f,\"structuralScore\":%.2f,\"cohesionScore\":%.2f"
                + ",\"issues\":[]}", name, score, score, score, score, score, score);
    }

    private void writeClarityJson(Path projectDir, String scenariosContent) throws IOException {
        writeClarityJsonRaw(projectDir,
                "{\"version\":\"1.0\",\"scenarios\":[" + scenariosContent + "]}");
    }

    private void writeClarityJsonRaw(Path projectDir, String json) throws IOException {
        var dir = projectDir.resolve("build/narrativetrace");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("clarity-results.json"), json);
    }

    private void writeJavaSource(Path projectDir) throws IOException {
        var srcDir = projectDir.resolve("src/main/java");
        Files.createDirectories(srcDir);
        Files.writeString(srcDir.resolve("Dummy.java"), "public class Dummy {}");
    }

    private void writeBuildFile(Path projectDir, String extraConfig) throws IOException {
        Files.writeString(projectDir.resolve("settings.gradle.kts"), "rootProject.name = \"test-project\"");
        Files.writeString(projectDir.resolve("build.gradle.kts"),
                "plugins {\n"
                + "    java\n"
                + "    id(\"ai.narrativetrace\")\n"
                + "}\n"
                + "repositories { mavenCentral() }\n"
                + extraConfig);
    }
}
