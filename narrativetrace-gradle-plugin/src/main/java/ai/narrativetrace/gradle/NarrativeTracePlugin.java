package ai.narrativetrace.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.testing.Test;

public class NarrativeTracePlugin implements Plugin<Project> {

    private static final String VERSION = "0.1.0-SNAPSHOT";
    private static final String GROUP = "ai.narrativetrace";

    @Override
    public void apply(Project project) {
        var extension = project.getExtensions().create("narrativeTrace", NarrativeTraceExtension.class);
        extension.getOutputDir().convention(project.getLayout().getBuildDirectory().dir("narrativetrace"));

        project.afterEvaluate(p -> {
            if (!extension.getEnabled().get()) {
                return;
            }
            configureCompilerFlags(p);
            if (extension.getManageDependencies().get()) {
                configureDependencies(p, extension);
            }
            configureTestTasks(p, extension);
            registerClarityCheckTask(p, extension);
            registerClarityScanTask(p, extension);
        });
    }

    private void configureCompilerFlags(Project project) {
        project.getTasks().withType(JavaCompile.class, task -> {
            var args = task.getOptions().getCompilerArgs();
            if (!args.contains("-parameters")) {
                args.add("-parameters");
            }
        });
    }

    private void configureDependencies(Project project, NarrativeTraceExtension extension) {
        var deps = project.getDependencies();
        deps.add("testImplementation", GROUP + ":narrativetrace-core:" + VERSION);
        deps.add("testImplementation", GROUP + ":narrativetrace-proxy:" + VERSION);
        deps.add("testImplementation", GROUP + ":narrativetrace-clarity:" + VERSION);
        deps.add("testImplementation", GROUP + ":narrativetrace-diagrams:" + VERSION);

        var framework = extension.getTestFramework().get();
        if ("junit4".equals(framework)) {
            deps.add("testImplementation", GROUP + ":narrativetrace-junit4:" + VERSION);
        } else {
            deps.add("testImplementation", GROUP + ":narrativetrace-junit5:" + VERSION);
        }
    }

    private void registerClarityCheckTask(Project project, NarrativeTraceExtension extension) {
        var clarityCheck = project.getTasks().register("clarityCheck", ClarityCheckTask.class, task -> {
            task.setDescription("Checks clarity scores against configured thresholds");
            task.setGroup("verification");
            task.getJsonFile().set(extension.getOutputDir().file("clarity-results.json"));
            task.getMinScore().set(extension.getClarity().getMinScore());
            task.getMaxHighIssues().set(extension.getClarity().getMaxHighIssues());
            task.getWarnOnly().set(extension.getClarity().getWarnOnly());
            task.dependsOn(project.getTasks().named("test"));
        });

        project.getPlugins().withId("java", plugin ->
                project.getTasks().named("check").configure(check ->
                        check.dependsOn(clarityCheck)));
    }

    private void registerClarityScanTask(Project project, NarrativeTraceExtension extension) {
        project.getTasks().register("clarityScan", JavaExec.class, task -> {
            task.setDescription("Analyzes naming clarity of compiled classes without running tests");
            task.setGroup("verification");
            task.getMainClass().set("ai.narrativetrace.clarity.ClarityScannerMain");

            var classesDir = project.getLayout().getBuildDirectory().dir("classes/java/main");
            task.args("--classes-dir", classesDir.get().getAsFile().getAbsolutePath(),
                    "--output-dir", extension.getOutputDir().get().getAsFile().getAbsolutePath());

            task.classpath(project.getConfigurations().getByName("testRuntimeClasspath"));
            task.dependsOn(project.getTasks().named("classes"));
        });
    }

    private void configureTestTasks(Project project, NarrativeTraceExtension extension) {
        project.getTasks().withType(Test.class, task -> {
            task.systemProperty("narrativetrace.output", "true");
            task.systemProperty("narrativetrace.outputDir",
                    extension.getOutputDir().get().getAsFile().getAbsolutePath());
        });
    }
}
