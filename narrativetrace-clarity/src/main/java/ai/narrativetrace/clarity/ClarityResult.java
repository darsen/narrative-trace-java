package ai.narrativetrace.clarity;

import java.util.List;

public record ClarityResult(
        double overallScore,
        double methodNameScore,
        double classNameScore,
        double parameterNameScore,
        double structuralScore,
        double cohesionScore,
        List<ClarityIssue> issues
) {
}
