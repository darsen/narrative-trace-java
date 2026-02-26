package ai.narrativetrace.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

public abstract class ClarityCheckTask extends DefaultTask {

    @Internal
    public abstract RegularFileProperty getJsonFile();

    @Input
    public abstract Property<Double> getMinScore();

    @Input
    public abstract Property<Integer> getMaxHighIssues();

    @Input
    public abstract Property<Boolean> getWarnOnly();

    @TaskAction
    public void check() {
        var file = getJsonFile().getAsFile().getOrNull();
        if (file == null || !file.exists()) {
            getLogger().lifecycle("NarrativeTrace: No clarity-results.json found — skipping clarity check.");
            return;
        }

        String json;
        try {
            json = Files.readString(file.toPath());
        } catch (IOException e) {
            throw new GradleException("Failed to read clarity results: " + e.getMessage(), e);
        }

        var scenarios = ClarityResultsParser.parse(json);
        var minScore = getMinScore().get();
        var maxHighIssues = getMaxHighIssues().get();
        var warnOnly = getWarnOnly().get();

        var failures = new ArrayList<String>();

        for (var scenario : scenarios) {
            if (scenario.overallScore() < minScore) {
                failures.add(String.format("  '%s': score %.2f < threshold %.2f",
                        scenario.name(), scenario.overallScore(), minScore));
            }
            long highCount = scenario.issues().stream()
                    .filter(i -> "HIGH".equals(i.severity()))
                    .count();
            if (highCount > maxHighIssues) {
                failures.add(String.format("  '%s': %d HIGH issues (max %d)",
                        scenario.name(), highCount, maxHighIssues));
            }
        }

        if (!failures.isEmpty()) {
            var message = "Clarity check failed:\n" + String.join("\n", failures);
            if (warnOnly) {
                getLogger().warn(message);
            } else {
                throw new GradleException(message);
            }
        }
    }
}
