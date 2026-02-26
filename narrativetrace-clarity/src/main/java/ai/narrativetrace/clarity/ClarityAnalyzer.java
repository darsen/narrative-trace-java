package ai.narrativetrace.clarity;

import ai.narrativetrace.core.event.TraceNode;
import ai.narrativetrace.core.tree.TraceTree;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ClarityAnalyzer {

    private static final double METHOD_WEIGHT = 0.30;
    private static final double CLASS_WEIGHT = 0.20;
    private static final double PARAM_WEIGHT = 0.25;
    private static final double STRUCTURAL_WEIGHT = 0.15;
    private static final double COHESION_WEIGHT = 0.10;

    private static final double HIGH_SEVERITY_THRESHOLD = 0.20;
    private static final double MEDIUM_SEVERITY_THRESHOLD = 0.50;

    private final MethodNameScorer methodNameScorer = new MethodNameScorer();
    private final ClassNameScorer classNameScorer = new ClassNameScorer();
    private final ParameterNameScorer parameterNameScorer = new ParameterNameScorer();
    private final CohesionScorer cohesionScorer = new CohesionScorer();
    private final IdentifierTokenizer tokenizer = new IdentifierTokenizer();
    private final CollocationDictionary collocationDictionary = new CollocationDictionary();

    public ClarityResult analyze(TraceTree tree) {
        var nodes = flattenNodes(tree.roots());

        double methodScore = averageMethodScore(nodes);
        double classScore = averageClassNameScore(nodes);
        double paramScore = averageParamScore(nodes);
        double structuralScore = structuralFactor(nodes, tree);
        double cohesionScore = computeCohesionScore(nodes);

        double overall = methodScore * METHOD_WEIGHT
                + classScore * CLASS_WEIGHT
                + paramScore * PARAM_WEIGHT
                + structuralScore * STRUCTURAL_WEIGHT
                + cohesionScore * COHESION_WEIGHT;

        var issues = collectAndDeduplicateIssues(nodes);

        return new ClarityResult(overall, methodScore, classScore, paramScore,
                structuralScore, cohesionScore, List.copyOf(issues));
    }

    private double averageMethodScore(List<TraceNode> nodes) {
        if (nodes.isEmpty()) return 0.0;
        return nodes.stream()
                .mapToDouble(n -> methodNameScorer.score(n.signature().methodName()))
                .average()
                .orElse(0.0);
    }

    private double averageClassNameScore(List<TraceNode> nodes) {
        var uniqueClassNames = nodes.stream()
                .map(n -> n.signature().className())
                .distinct()
                .toList();
        if (uniqueClassNames.isEmpty()) return 0.0;
        return uniqueClassNames.stream()
                .mapToDouble(classNameScorer::score)
                .average()
                .orElse(0.0);
    }

    private double averageParamScore(List<TraceNode> nodes) {
        var allParams = nodes.stream()
                .flatMap(n -> n.signature().parameters().stream())
                .toList();
        if (allParams.isEmpty()) return 1.0;
        return allParams.stream()
                .mapToDouble(p -> parameterNameScorer.score(p.name()))
                .average()
                .orElse(0.0);
    }

    private double structuralFactor(List<TraceNode> nodes, TraceTree tree) {
        int maxParams = maxParamCount(nodes);
        int depth = maxDepth(tree.roots(), 1);

        double penalty = 0.0;
        if (maxParams > 4) penalty += 0.1 * (maxParams - 4);
        if (depth > 5) penalty += 0.05 * (depth - 5);

        return Math.max(0.0, 1.0 - penalty);
    }

    private double computeCohesionScore(List<TraceNode> nodes) {
        if (nodes.isEmpty()) return 0.7;

        Map<String, List<String>> classMethods = nodes.stream()
                .collect(Collectors.groupingBy(
                        n -> n.signature().className(),
                        Collectors.mapping(n -> n.signature().methodName(), Collectors.toList())
                ));

        return cohesionScorer.scoreTrace(classMethods);
    }

    private int maxParamCount(List<TraceNode> nodes) {
        return nodes.stream()
                .mapToInt(n -> n.signature().parameters().size())
                .max()
                .orElse(0);
    }

    private int maxDepth(List<TraceNode> nodes, int currentDepth) {
        if (nodes.isEmpty()) return currentDepth - 1;
        int max = currentDepth;
        for (var node : nodes) {
            max = Math.max(max, maxDepth(node.children(), currentDepth + 1));
        }
        return max;
    }

    private List<ClarityIssue> collectAndDeduplicateIssues(List<TraceNode> nodes) {
        var rawIssues = new ArrayList<ClarityIssue>();
        rawIssues.addAll(findMethodNameIssues(nodes));
        rawIssues.addAll(findClassNameIssues(nodes));
        rawIssues.addAll(findParamNameIssues(nodes));
        rawIssues.addAll(findCollocationIssues(nodes));

        return deduplicateAndRank(rawIssues);
    }

    private List<ClarityIssue> deduplicateAndRank(List<ClarityIssue> issues) {
        var grouped = new HashMap<String, List<ClarityIssue>>();
        for (var issue : issues) {
            var key = issue.category() + "|" + issue.element();
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(issue);
        }

        return grouped.values().stream()
                .map(group -> {
                    var first = group.get(0);
                    return first.withOccurrences(group.size());
                })
                .sorted(Comparator.comparingDouble(ClarityIssue::impactScore).reversed())
                .toList();
    }

    private ClarityIssue.Severity classifySeverity(double score) {
        if (score <= HIGH_SEVERITY_THRESHOLD) return ClarityIssue.Severity.HIGH;
        if (score <= MEDIUM_SEVERITY_THRESHOLD) return ClarityIssue.Severity.MEDIUM;
        return ClarityIssue.Severity.LOW;
    }

    private List<ClarityIssue> findMethodNameIssues(List<TraceNode> nodes) {
        var issues = new ArrayList<ClarityIssue>();
        for (var node : nodes) {
            var sig = node.signature();
            double score = methodNameScorer.score(sig.methodName());
            var severity = classifySeverity(score);
            if (score < MEDIUM_SEVERITY_THRESHOLD) {
                issues.add(new ClarityIssue(
                        "method-name",
                        sig.className() + "." + sig.methodName(),
                        "Use a domain-specific verb+noun (e.g., calculateTotal, reserveInventory)",
                        severity, 1, severity.weight()
                ));
            }
        }
        return issues;
    }

    private List<ClarityIssue> findClassNameIssues(List<TraceNode> nodes) {
        var issues = new ArrayList<ClarityIssue>();
        var seen = new HashSet<String>();
        for (var node : nodes) {
            var className = node.signature().className();
            if (seen.add(className)) {
                double score = classNameScorer.score(className);
                var severity = classifySeverity(score);
                if (score < MEDIUM_SEVERITY_THRESHOLD) {
                    issues.add(new ClarityIssue(
                            "class-name",
                            className,
                            "Use a domain-specific name or a recognized pattern suffix (e.g., OrderService, PaymentGateway)",
                            severity, 1, severity.weight()
                    ));
                }
            }
        }
        return issues;
    }

    private List<ClarityIssue> findParamNameIssues(List<TraceNode> nodes) {
        var issues = new ArrayList<ClarityIssue>();
        for (var node : nodes) {
            for (var param : node.signature().parameters()) {
                double score = parameterNameScorer.score(param.name());
                var severity = classifySeverity(score);
                if (score < MEDIUM_SEVERITY_THRESHOLD) {
                    issues.add(new ClarityIssue(
                            "param-name",
                            param.name(),
                            "Use a domain-specific name (e.g., customerId, orderAmount)",
                            severity, 1, severity.weight()
                    ));
                }
            }
        }
        return issues;
    }

    private List<ClarityIssue> findCollocationIssues(List<TraceNode> nodes) {
        var issues = new ArrayList<ClarityIssue>();
        for (var node : nodes) {
            var sig = node.signature();
            var tokens = tokenizer.tokenize(sig.methodName());
            if (tokens.size() < 2) continue;

            var verb = tokens.get(0).toLowerCase();
            var noun = tokens.get(tokens.size() - 1).toLowerCase();
            var preferred = collocationDictionary.preferredVerbs(noun);
            if (preferred.isEmpty() || preferred.contains(verb)) continue;

            var capitalNoun = Character.toUpperCase(noun.charAt(0)) + noun.substring(1);
            var suggestion = preferred.stream()
                    .sorted()
                    .map(v -> v + capitalNoun)
                    .collect(Collectors.joining(", "));
            issues.add(new ClarityIssue(
                    "collocation",
                    sig.className() + "." + sig.methodName(),
                    "Consider: " + suggestion,
                    ClarityIssue.Severity.LOW, 1, ClarityIssue.Severity.LOW.weight()
            ));
        }
        return issues;
    }

    private List<TraceNode> flattenNodes(List<TraceNode> nodes) {
        var result = new ArrayList<TraceNode>();
        for (var node : nodes) {
            result.add(node);
            result.addAll(flattenNodes(node.children()));
        }
        return result;
    }
}
