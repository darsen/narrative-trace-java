package ai.narrativetrace.clarity;

import ai.narrativetrace.core.event.MethodSignature;
import ai.narrativetrace.core.event.ParameterCapture;
import ai.narrativetrace.core.event.TraceNode;
import ai.narrativetrace.core.event.TraceOutcome;
import ai.narrativetrace.core.tree.DefaultTraceTree;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ClarityAnalyzerTest {

    private final ClarityAnalyzer analyzer = new ClarityAnalyzer();

    @Test
    void analyzesTraceTreeAndProducesClarityResult() {
        var node = new TraceNode(
                new MethodSignature("OrderService", "calculateTotal", List.of(
                        new ParameterCapture("customerId", "\"C-123\"", false),
                        new ParameterCapture("orderAmount", "99.0", false)
                )),
                List.of(),
                new TraceOutcome.Returned("99.0"),
                10_000_000L
        );
        var tree = new DefaultTraceTree(List.of(node));

        var result = analyzer.analyze(tree);

        assertThat(result.overallScore()).isGreaterThan(0.85);
        assertThat(result.methodNameScore()).isGreaterThanOrEqualTo(0.90);
        assertThat(result.parameterNameScore()).isGreaterThan(0.85);
        assertThat(result.structuralScore()).isCloseTo(1.0, within(0.01));
        assertThat(result.issues()).isEmpty();
    }

    @Test
    void identifiesGenericMethodNameIssue() {
        var node = new TraceNode(
                new MethodSignature("Service", "process", List.of()),
                List.of(),
                new TraceOutcome.Returned("true"),
                1_000_000L
        );
        var tree = new DefaultTraceTree(List.of(node));

        var result = analyzer.analyze(tree);

        assertThat(result.methodNameScore()).isLessThanOrEqualTo(0.15);
        assertThat(result.issues()).anyMatch(i ->
                i.category().equals("method-name") && i.element().equals("Service.process"));
    }

    @Test
    void identifiesMeaninglessParameterNameIssue() {
        var node = new TraceNode(
                new MethodSignature("OrderService", "calculateTotal", List.of(
                        new ParameterCapture("data", "\"something\"", false),
                        new ParameterCapture("obj", "\"something\"", false)
                )),
                List.of(),
                new TraceOutcome.Returned("99.0"),
                1_000_000L
        );
        var tree = new DefaultTraceTree(List.of(node));

        var result = analyzer.analyze(tree);

        assertThat(result.parameterNameScore()).isLessThanOrEqualTo(0.15);
        assertThat(result.issues()).anyMatch(i ->
                i.category().equals("param-name") && i.element().equals("data"));
        assertThat(result.issues()).anyMatch(i ->
                i.category().equals("param-name") && i.element().equals("obj"));
    }

    @Test
    void emptyTreeReturnsZeroMethodScore() {
        var tree = new DefaultTraceTree(List.of());

        var result = analyzer.analyze(tree);

        assertThat(result.methodNameScore()).isEqualTo(0.0);
        assertThat(result.issues()).isEmpty();
    }

    @Test
    void identifiesGenericClassNameIssue() {
        var node = new TraceNode(
                new MethodSignature("Manager", "process", List.of()),
                List.of(),
                new TraceOutcome.Returned("true"),
                1_000_000L
        );
        var tree = new DefaultTraceTree(List.of(node));

        var result = analyzer.analyze(tree);

        assertThat(result.issues()).anyMatch(i ->
                i.category().equals("class-name") && i.element().equals("Manager"));
    }

    @Test
    void classNameScoreContributesToOverall() {
        var node = new TraceNode(
                new MethodSignature("ShoppingCart", "calculateTotal", List.of(
                        new ParameterCapture("customerId", "\"C-123\"", false),
                        new ParameterCapture("orderAmount", "99.0", false)
                )),
                List.of(),
                new TraceOutcome.Returned("99.0"),
                10_000_000L
        );
        var tree = new DefaultTraceTree(List.of(node));

        var result = analyzer.analyze(tree);

        assertThat(result.classNameScore()).isGreaterThanOrEqualTo(0.85);
        assertThat(result.overallScore()).isGreaterThan(0.85);
    }

    @Test
    void penalizesHighParameterCountAndDeepNesting() {
        var params = List.of(
                new ParameterCapture("a1", "\"v\"", false),
                new ParameterCapture("a2", "\"v\"", false),
                new ParameterCapture("a3", "\"v\"", false),
                new ParameterCapture("a4", "\"v\"", false),
                new ParameterCapture("a5", "\"v\"", false),
                new ParameterCapture("a6", "\"v\"", false)
        );

        var leaf = new TraceNode(
                new MethodSignature("S", "calculateTotal", params),
                List.of(), new TraceOutcome.Returned("\"ok\""), 1_000_000L);
        var current = leaf;
        for (int i = 0; i < 5; i++) {
            current = new TraceNode(
                    new MethodSignature("S", "calculateTotal", List.of()),
                    List.of(current), new TraceOutcome.Returned("\"ok\""), 1_000_000L);
        }
        var tree = new DefaultTraceTree(List.of(current));

        var result = analyzer.analyze(tree);

        assertThat(result.structuralScore()).isLessThan(1.0);
    }

    @Test
    void includesCohesionScoreInResult() {
        var node = new TraceNode(
                new MethodSignature("OrderRepository", "findById", List.of(
                        new ParameterCapture("orderId", "\"O-1\"", false)
                )),
                List.of(),
                new TraceOutcome.Returned("\"order\""),
                1_000_000L
        );
        var tree = new DefaultTraceTree(List.of(node));

        var result = analyzer.analyze(tree);

        assertThat(result.cohesionScore()).isGreaterThan(0.0);
    }

    @Test
    void weighsCohesionAt10Percent() {
        var node = new TraceNode(
                new MethodSignature("OrderService", "calculateTotal", List.of()),
                List.of(),
                new TraceOutcome.Returned("99.0"),
                1_000_000L
        );
        var tree = new DefaultTraceTree(List.of(node));

        var result = analyzer.analyze(tree);

        double expectedContribution = result.cohesionScore() * 0.10;
        double actualContribution = result.overallScore()
                - result.methodNameScore() * 0.30
                - result.classNameScore() * 0.20
                - result.parameterNameScore() * 0.25
                - result.structuralScore() * 0.15;
        assertThat(actualContribution).isCloseTo(expectedContribution, within(0.01));
    }

    @Test
    void deduplicatesIssuesInResult() {
        var node1 = new TraceNode(
                new MethodSignature("OrderService", "calculateTotal", List.of(
                        new ParameterCapture("data", "\"x\"", false)
                )),
                List.of(),
                new TraceOutcome.Returned("99.0"),
                1_000_000L
        );
        var node2 = new TraceNode(
                new MethodSignature("OrderService", "validateOrder", List.of(
                        new ParameterCapture("data", "\"y\"", false)
                )),
                List.of(),
                new TraceOutcome.Returned("true"),
                1_000_000L
        );
        var tree = new DefaultTraceTree(List.of(node1, node2));

        var result = analyzer.analyze(tree);

        long dataIssues = result.issues().stream()
                .filter(i -> i.element().equals("data"))
                .count();
        assertThat(dataIssues).isEqualTo(1);
        var dataIssue = result.issues().stream()
                .filter(i -> i.element().equals("data"))
                .findFirst()
                .orElseThrow();
        assertThat(dataIssue.occurrences()).isEqualTo(2);
    }

    @Test
    void ranksIssuesByImpact() {
        var node = new TraceNode(
                new MethodSignature("Manager", "process", List.of(
                        new ParameterCapture("data", "\"x\"", false)
                )),
                List.of(),
                new TraceOutcome.Returned("true"),
                1_000_000L
        );
        var tree = new DefaultTraceTree(List.of(node));

        var result = analyzer.analyze(tree);

        if (result.issues().size() >= 2) {
            for (int i = 0; i < result.issues().size() - 1; i++) {
                assertThat(result.issues().get(i).impactScore())
                        .isGreaterThanOrEqualTo(result.issues().get(i + 1).impactScore());
            }
        }
    }

    @Test
    void reportsCollocationIssueForNonPreferredVerb() {
        var node = new TraceNode(
                new MethodSignature("LedgerService", "checkLedger", List.of()),
                List.of(),
                new TraceOutcome.Returned("true"),
                1_000_000L
        );
        var tree = new DefaultTraceTree(List.of(node));

        var result = analyzer.analyze(tree);

        var collocationIssue = result.issues().stream()
                .filter(i -> i.category().equals("collocation"))
                .findFirst();
        assertThat(collocationIssue).isPresent();
        assertThat(collocationIssue.get().element()).isEqualTo("LedgerService.checkLedger");
        assertThat(collocationIssue.get().suggestion()).contains("reconcileLedger");
        assertThat(collocationIssue.get().severity()).isEqualTo(ClarityIssue.Severity.LOW);
    }

    @Test
    void noCollocationIssueForPreferredVerb() {
        var node = new TraceNode(
                new MethodSignature("LedgerService", "reconcileLedger", List.of()),
                List.of(),
                new TraceOutcome.Returned("true"),
                1_000_000L
        );
        var tree = new DefaultTraceTree(List.of(node));

        var result = analyzer.analyze(tree);

        assertThat(result.issues().stream()
                .filter(i -> i.category().equals("collocation"))
                .findFirst()).isEmpty();
    }

    @Test
    void noCollocationIssueForUnknownNoun() {
        var node = new TraceNode(
                new MethodSignature("FooService", "processWidget", List.of()),
                List.of(),
                new TraceOutcome.Returned("true"),
                1_000_000L
        );
        var tree = new DefaultTraceTree(List.of(node));

        var result = analyzer.analyze(tree);

        assertThat(result.issues().stream()
                .filter(i -> i.category().equals("collocation"))
                .findFirst()).isEmpty();
    }

    @Test
    void overallScoreIsContinuous() {
        var node1 = new TraceNode(
                new MethodSignature("OrderService", "getCustomer", List.of()),
                List.of(),
                new TraceOutcome.Returned("\"c\""),
                1_000_000L
        );
        var node2 = new TraceNode(
                new MethodSignature("OrderService", "processOrder", List.of()),
                List.of(),
                new TraceOutcome.Returned("\"o\""),
                1_000_000L
        );
        var tree1 = new DefaultTraceTree(List.of(node1));
        var tree2 = new DefaultTraceTree(List.of(node2));

        var result1 = analyzer.analyze(tree1);
        var result2 = analyzer.analyze(tree2);

        // These should produce different scores (not bucket-collapsed)
        assertThat(result1.methodNameScore()).isNotEqualTo(result2.methodNameScore());
    }
}
