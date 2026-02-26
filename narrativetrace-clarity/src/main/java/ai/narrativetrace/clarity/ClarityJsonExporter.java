package ai.narrativetrace.clarity;

import java.util.Map;

public final class ClarityJsonExporter {

    public String export(Map<String, ClarityResult> results) {
        var sb = new StringBuilder();
        sb.append("{\"version\":\"1.0\",\"scenarios\":[");

        boolean first = true;
        for (var entry : results.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            appendScenario(sb, entry.getKey(), entry.getValue());
        }

        sb.append("]}");
        return sb.toString();
    }

    private void appendScenario(StringBuilder sb, String name, ClarityResult result) {
        sb.append("{\"name\":\"").append(escapeJson(name)).append('"');
        sb.append(",\"overallScore\":").append(formatScore(result.overallScore()));
        sb.append(",\"methodNameScore\":").append(formatScore(result.methodNameScore()));
        sb.append(",\"classNameScore\":").append(formatScore(result.classNameScore()));
        sb.append(",\"parameterNameScore\":").append(formatScore(result.parameterNameScore()));
        sb.append(",\"structuralScore\":").append(formatScore(result.structuralScore()));
        sb.append(",\"cohesionScore\":").append(formatScore(result.cohesionScore()));
        sb.append(",\"issues\":[");
        boolean firstIssue = true;
        for (var issue : result.issues()) {
            if (!firstIssue) {
                sb.append(',');
            }
            firstIssue = false;
            appendIssue(sb, issue);
        }
        sb.append("]}");
    }

    private void appendIssue(StringBuilder sb, ClarityIssue issue) {
        sb.append("{\"category\":\"").append(escapeJson(issue.category())).append('"');
        sb.append(",\"element\":\"").append(escapeJson(issue.element())).append('"');
        sb.append(",\"suggestion\":\"").append(escapeJson(issue.suggestion())).append('"');
        sb.append(",\"severity\":\"").append(issue.severity().name()).append('"');
        sb.append(",\"occurrences\":").append(issue.occurrences());
        sb.append(",\"impactScore\":").append(formatScore(issue.impactScore()));
        sb.append('}');
    }

    private String formatScore(double score) {
        return String.format("%.2f", score);
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
