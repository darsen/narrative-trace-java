package ai.narrativetrace.clarity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ClarityReportRendererTest {

  private final ClarityReportRenderer renderer = new ClarityReportRenderer();

  @Test
  void rendersOverallScoreTable() {
    var result = new ClarityResult(0.85, 1.0, 0.9, 0.7, 0.8, 0.9, List.of());

    var markdown = renderer.render("OrderService.calculateTotal", result);

    assertThat(markdown).contains("# Clarity Report: OrderService.calculateTotal");
    assertThat(markdown).contains("| Category | Score | Weight | Weighted |");
    assertThat(markdown).contains("| Method Names | 1.00 | 0.30 | 0.30 |");
    assertThat(markdown).contains("| Class Names | 0.90 | 0.20 | 0.18 |");
    assertThat(markdown).contains("| Parameter Names | 0.70 | 0.25 | 0.18 |");
    assertThat(markdown).contains("| Structural | 0.80 | 0.15 | 0.12 |");
    assertThat(markdown).contains("| Cohesion | 0.90 | 0.10 | 0.09 |");
    assertThat(markdown).contains("| **Overall** |");
    assertThat(markdown).contains("| **0.85** |");
  }

  @Test
  void rendersIssuesTableWithSuggestions() {
    var issues =
        List.of(
            new ClarityIssue(
                "method-name",
                "Service.process",
                "Use a domain-specific verb+noun",
                ClarityIssue.Severity.HIGH,
                1,
                3.0),
            new ClarityIssue(
                "param-name",
                "data",
                "Use a domain-specific name",
                ClarityIssue.Severity.HIGH,
                1,
                3.0));
    var result = new ClarityResult(0.4, 0.0, 0.0, 0.0, 1.0, 0.7, issues);

    var markdown = renderer.render("Service.process", result);

    assertThat(markdown).contains("## Issues");
    assertThat(markdown).contains("| Severity | Category | Element | Suggestion |");
    assertThat(markdown).contains("| HIGH | method-name | `Service.process` |");
    assertThat(markdown).contains("| HIGH | param-name | `data` |");
  }

  @Test
  void rendersSuiteReportWithLowestClarityScenarios() {
    var highClarity = new ClarityResult(0.95, 1.0, 1.0, 0.9, 1.0, 1.0, List.of());
    var lowClarity =
        new ClarityResult(
            0.25,
            0.0,
            0.0,
            0.0,
            1.0,
            0.7,
            List.of(
                new ClarityIssue(
                    "method-name",
                    "Service.process",
                    "Use a domain-specific verb+noun",
                    ClarityIssue.Severity.HIGH,
                    1,
                    3.0)));
    var results =
        Map.of(
            "OrderService.calculateTotal", highClarity,
            "Service.process", lowClarity);

    var markdown = renderer.renderSuiteReport(results);

    assertThat(markdown).contains("# Clarity Suite Report");
    assertThat(markdown).contains("| Scenario | Score |");
    assertThat(markdown).contains("| Service.process | 0.25 |");
    assertThat(markdown).contains("| OrderService.calculateTotal | 0.95 |");
    assertThat(markdown).contains("### Service.process");
    assertThat(markdown).contains("| HIGH | method-name | `Service.process` |");
  }

  @Test
  void rendersCohesionRow() {
    var result = new ClarityResult(0.85, 1.0, 0.9, 0.7, 0.8, 0.9, List.of());

    var markdown = renderer.render("TestScenario", result);

    assertThat(markdown).contains("| Cohesion | 0.90 | 0.10 |");
  }

  @Test
  void rendersSeverityColumn() {
    var issues =
        List.of(
            new ClarityIssue(
                "method-name", "Service.process", "Fix it", ClarityIssue.Severity.HIGH, 1, 3.0));
    var result = new ClarityResult(0.4, 0.0, 0.0, 0.0, 1.0, 0.7, issues);

    var markdown = renderer.render("Test", result);

    assertThat(markdown).contains("| Severity |");
    assertThat(markdown).contains("| HIGH |");
  }

  @Test
  void rendersIssuesSortedByImpact() {
    var issues =
        List.of(
            new ClarityIssue("param-name", "count", "low", ClarityIssue.Severity.LOW, 1, 1.0),
            new ClarityIssue("method-name", "process", "high", ClarityIssue.Severity.HIGH, 3, 9.0));
    var result = new ClarityResult(0.3, 0.0, 0.0, 0.0, 1.0, 0.7, issues);

    var markdown = renderer.render("Test", result);

    int processPos = markdown.indexOf("`process`");
    int countPos = markdown.indexOf("`count`");
    assertThat(processPos).isLessThan(countPos);
  }

  @Test
  void rendersOccurrencesCount() {
    var issues =
        List.of(new ClarityIssue("param-name", "data", "fix", ClarityIssue.Severity.HIGH, 3, 9.0));
    var result = new ClarityResult(0.3, 0.0, 0.0, 0.0, 1.0, 0.7, issues);

    var markdown = renderer.render("Test", result);

    assertThat(markdown).contains("(x3)");
  }

  @Test
  void suiteReportIncludesCohesion() {
    var lowClarity =
        new ClarityResult(
            0.25,
            0.0,
            0.0,
            0.0,
            1.0,
            0.3,
            List.of(
                new ClarityIssue(
                    "method-name", "Service.process", "fix", ClarityIssue.Severity.HIGH, 1, 3.0)));
    var results = Map.of("Service.process", lowClarity);

    var markdown = renderer.renderSuiteReport(results);

    assertThat(markdown).contains("| Cohesion | 0.30 | 0.10 |");
  }

  @Test
  void suiteReportSkipsDetailSectionForLowScoreWithNoIssues() {
    var lowNoIssues = new ClarityResult(0.3, 0.3, 0.3, 0.3, 0.3, 0.3, List.of());
    var results = Map.of("Service.empty", lowNoIssues);

    var markdown = renderer.renderSuiteReport(results);

    assertThat(markdown).contains("| Service.empty | 0.30 |");
    assertThat(markdown).doesNotContain("### Service.empty");
  }
}
