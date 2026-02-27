package ai.narrativetrace.clarity;

import java.util.List;

/**
 * Result of a clarity analysis containing overall and per-dimension scores plus issues.
 *
 * <p>All scores range from 0.0 (poor) to 1.0 (excellent). Typical thresholds:
 *
 * <ul>
 *   <li>0.80+ — good naming, no action needed
 *   <li>0.60-0.79 — acceptable, minor improvements suggested
 *   <li>below 0.60 — poor naming, significant refactoring recommended
 * </ul>
 *
 * @param overallScore weighted combination of all dimension scores
 * @param methodNameScore average quality of method names (verb+noun, domain vocabulary)
 * @param classNameScore average quality of class names (role suffixes, domain terms)
 * @param parameterNameScore average quality of parameter names (specificity, no abbreviations)
 * @param structuralScore penalty for high parameter counts and deep call chains
 * @param cohesionScore vocabulary consistency within each class
 * @param issues ranked list of specific naming problems with suggestions
 */
public record ClarityResult(
    double overallScore,
    double methodNameScore,
    double classNameScore,
    double parameterNameScore,
    double structuralScore,
    double cohesionScore,
    List<ClarityIssue> issues) {}
