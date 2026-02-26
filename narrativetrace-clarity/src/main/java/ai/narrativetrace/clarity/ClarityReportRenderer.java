package ai.narrativetrace.clarity;

import java.util.Comparator;
import java.util.Map;

public final class ClarityReportRenderer {

    private static final double LOW_SCORE_THRESHOLD = 0.7;

    public String render(String scenarioName, ClarityResult result) {
        var sb = new StringBuilder();

        sb.append("# Clarity Report: ").append(scenarioName).append("\n\n");

        appendScoresTable(sb, result);

        if (!result.issues().isEmpty()) {
            sb.append("\n## Issues\n\n");
            appendIssuesTable(sb, result);
        }

        return sb.toString().stripTrailing();
    }

    public String renderSuiteReport(Map<String, ClarityResult> results) {
        var sb = new StringBuilder();

        sb.append("# Clarity Suite Report\n\n");

        sb.append("## Scenarios\n\n");
        sb.append("| Scenario | Score |\n");
        sb.append("|----------|-------|\n");

        var sorted = results.entrySet().stream()
                .sorted(Comparator.comparingDouble(e -> e.getValue().overallScore()))
                .toList();

        for (var entry : sorted) {
            sb.append(String.format("| %s | %.2f |%n", entry.getKey(), entry.getValue().overallScore()));
        }

        for (var entry : sorted) {
            if (entry.getValue().overallScore() < LOW_SCORE_THRESHOLD && !entry.getValue().issues().isEmpty()) {
                sb.append(String.format("%n### %s%n%n", entry.getKey()));
                appendScoresTable(sb, entry.getValue());
                sb.append("\n");
                appendIssuesTable(sb, entry.getValue());
            }
        }

        return sb.toString().stripTrailing();
    }

    private void appendScoresTable(StringBuilder sb, ClarityResult result) {
        sb.append("## Scores\n\n");
        sb.append("| Category | Score | Weight | Weighted |\n");
        sb.append("|----------|-------|--------|----------|\n");
        sb.append(String.format("| Method Names | %.2f | 0.30 | %.2f |%n",
                result.methodNameScore(), result.methodNameScore() * 0.30));
        sb.append(String.format("| Class Names | %.2f | 0.20 | %.2f |%n",
                result.classNameScore(), result.classNameScore() * 0.20));
        sb.append(String.format("| Parameter Names | %.2f | 0.25 | %.2f |%n",
                result.parameterNameScore(), result.parameterNameScore() * 0.25));
        sb.append(String.format("| Structural | %.2f | 0.15 | %.2f |%n",
                result.structuralScore(), result.structuralScore() * 0.15));
        sb.append(String.format("| Cohesion | %.2f | 0.10 | %.2f |%n",
                result.cohesionScore(), result.cohesionScore() * 0.10));
        sb.append(String.format("| **Overall** | **%.2f** | | |%n", result.overallScore()));
    }

    private void appendIssuesTable(StringBuilder sb, ClarityResult result) {
        var sorted = result.issues().stream()
                .sorted((a, b) -> Double.compare(b.impactScore(), a.impactScore()))
                .toList();

        sb.append("| Severity | Category | Element | Suggestion |\n");
        sb.append("|----------|----------|---------|------------|\n");
        for (var issue : sorted) {
            String elementDisplay = issue.occurrences() > 1
                    ? String.format("`%s` (x%d)", issue.element(), issue.occurrences())
                    : String.format("`%s`", issue.element());
            sb.append(String.format("| %s | %s | %s | %s |%n",
                    issue.severity(), issue.category(), elementDisplay, issue.suggestion()));
        }
    }
}
