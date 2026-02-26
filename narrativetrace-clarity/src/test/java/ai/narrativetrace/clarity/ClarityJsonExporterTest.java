package ai.narrativetrace.clarity;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ClarityJsonExporterTest {

    private final ClarityJsonExporter exporter = new ClarityJsonExporter();

    @Test
    void emptyMapProducesEmptyScenarios() {
        String json = exporter.export(Map.of());

        assertThat(json).isEqualTo("{\"version\":\"1.0\",\"scenarios\":[]}");
    }

    @Test
    void singleScenarioWithScoresNoIssues() {
        var result = new ClarityResult(0.85, 0.90, 0.95, 0.80, 1.00, 0.70, List.of());
        var results = Map.of("Customer places order", result);

        String json = exporter.export(results);

        assertThat(json).isEqualTo(
                "{\"version\":\"1.0\",\"scenarios\":["
                + "{\"name\":\"Customer places order\""
                + ",\"overallScore\":0.85"
                + ",\"methodNameScore\":0.90"
                + ",\"classNameScore\":0.95"
                + ",\"parameterNameScore\":0.80"
                + ",\"structuralScore\":1.00"
                + ",\"cohesionScore\":0.70"
                + ",\"issues\":[]"
                + "}"
                + "]}");
    }

    @Test
    void scenarioWithIssuesSerialized() {
        var issue = new ClarityIssue(
                "param-name", "data", "Use a domain-specific name",
                ClarityIssue.Severity.MEDIUM, 2, 4.0);
        var result = new ClarityResult(0.65, 0.70, 0.80, 0.50, 1.00, 0.60, List.of(issue));
        var results = Map.of("Checkout flow", result);

        String json = exporter.export(results);

        assertThat(json).contains("\"issues\":[{")
                .contains("\"category\":\"param-name\"")
                .contains("\"element\":\"data\"")
                .contains("\"suggestion\":\"Use a domain-specific name\"")
                .contains("\"severity\":\"MEDIUM\"")
                .contains("\"occurrences\":2")
                .contains("\"impactScore\":4.00");
    }

    @Test
    void multipleScenariosPreserveOrder() {
        var results = new LinkedHashMap<String, ClarityResult>();
        results.put("Alpha", new ClarityResult(0.90, 0.90, 0.90, 0.90, 0.90, 0.90, List.of()));
        results.put("Beta", new ClarityResult(0.80, 0.80, 0.80, 0.80, 0.80, 0.80, List.of()));

        String json = exporter.export(results);

        int alphaIndex = json.indexOf("\"name\":\"Alpha\"");
        int betaIndex = json.indexOf("\"name\":\"Beta\"");
        assertThat(alphaIndex).isLessThan(betaIndex);
        assertThat(json).contains("},{");
    }

    @Test
    void specialCharactersEscaped() {
        var issue = new ClarityIssue(
                "method-name", "do\"stuff", "Use a \\proper\\ name",
                ClarityIssue.Severity.LOW, 1, 1.0);
        var result = new ClarityResult(0.50, 0.50, 0.50, 0.50, 0.50, 0.50, List.of(issue));
        var results = Map.of("Scenario with \"quotes\"", result);

        String json = exporter.export(results);

        assertThat(json)
                .contains("\"name\":\"Scenario with \\\"quotes\\\"\"")
                .contains("\"element\":\"do\\\"stuff\"")
                .contains("\"suggestion\":\"Use a \\\\proper\\\\ name\"");
    }
}
