package ai.narrativetrace.gradle;

import groovy.json.JsonSlurper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ClarityResultsParser {

    private ClarityResultsParser() {
    }

    @SuppressWarnings("unchecked")
    public static List<ClarityScenarioResult> parse(String json) {
        Map<String, Object> parsed;
        try {
            parsed = (Map<String, Object>) new JsonSlurper().parseText(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse clarity results: " + e.getMessage(), e);
        }
        if (!parsed.containsKey("version")) {
            throw new IllegalArgumentException("Failed to parse clarity results: missing 'version' field");
        }
        var scenarios = (List<Map<String, Object>>) parsed.get("scenarios");
        var results = new ArrayList<ClarityScenarioResult>();
        for (var scenario : scenarios) {
            results.add(parseScenario(scenario));
        }
        return results;
    }

    @SuppressWarnings("unchecked")
    private static ClarityScenarioResult parseScenario(Map<String, Object> scenario) {
        var issuesList = (List<Map<String, Object>>) scenario.get("issues");
        var issues = new ArrayList<ClarityScenarioResult.Issue>();
        for (var issue : issuesList) {
            issues.add(parseIssue(issue));
        }
        return new ClarityScenarioResult(
                (String) scenario.get("name"),
                toDouble(scenario.get("overallScore")),
                toDouble(scenario.get("methodNameScore")),
                toDouble(scenario.get("classNameScore")),
                toDouble(scenario.get("parameterNameScore")),
                toDouble(scenario.get("structuralScore")),
                toDouble(scenario.get("cohesionScore")),
                List.copyOf(issues)
        );
    }

    private static ClarityScenarioResult.Issue parseIssue(Map<String, Object> issue) {
        return new ClarityScenarioResult.Issue(
                (String) issue.get("category"),
                (String) issue.get("element"),
                (String) issue.get("suggestion"),
                (String) issue.get("severity"),
                ((Number) issue.get("occurrences")).intValue(),
                toDouble(issue.get("impactScore"))
        );
    }

    private static double toDouble(Object value) {
        return ((Number) value).doubleValue();
    }
}
