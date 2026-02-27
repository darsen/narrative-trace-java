package ai.narrativetrace.gradle;

import java.util.List;

public record ClarityScenarioResult(
    String name,
    double overallScore,
    double methodNameScore,
    double classNameScore,
    double parameterNameScore,
    double structuralScore,
    double cohesionScore,
    List<Issue> issues) {
  public record Issue(
      String category,
      String element,
      String suggestion,
      String severity,
      int occurrences,
      double impactScore) {}
}
