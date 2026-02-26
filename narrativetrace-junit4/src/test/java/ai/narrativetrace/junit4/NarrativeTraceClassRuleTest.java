package ai.narrativetrace.junit4;

import ai.narrativetrace.core.event.MethodSignature;
import ai.narrativetrace.core.event.TraceNode;
import ai.narrativetrace.core.event.TraceOutcome;
import ai.narrativetrace.core.tree.DefaultTraceTree;
import ai.narrativetrace.core.tree.TraceTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NarrativeTraceClassRuleTest {

    @BeforeEach
    void resetGlobal() {
        NarrativeTraceClassRule.resetGlobalAccumulator();
    }

    @Test
    void testRuleReturnsLinkedRule() {
        var classRule = new NarrativeTraceClassRule();
        var rule = classRule.testRule();

        assertThat(rule).isNotNull();
        assertThat(rule).isInstanceOf(NarrativeTraceRule.class);
    }

    @Test
    void linkedRuleAccumulatesTracesInClassRule() {
        var classRule = new NarrativeTraceClassRule();
        var rule = classRule.testRule();

        var desc = Description.createTestDescription(getClass(), "customerPlacesOrder");
        rule.starting(desc);
        rule.context().enterMethod(new MethodSignature("Service", "doWork", List.of()));
        rule.context().exitMethodWithReturn("ok");
        rule.finished(desc);

        assertThat(classRule.accumulatedTraces()).hasSize(1);
        assertThat(classRule.accumulatedTraces()).containsKey("Customer places order");
    }

    @Test
    void multipleTestsAccumulate() {
        var classRule = new NarrativeTraceClassRule();
        var rule = classRule.testRule();

        var desc1 = Description.createTestDescription(getClass(), "customerPlacesOrder");
        rule.starting(desc1);
        rule.context().enterMethod(new MethodSignature("OrderService", "placeOrder", List.of()));
        rule.context().exitMethodWithReturn("order-1");
        rule.finished(desc1);

        var desc2 = Description.createTestDescription(getClass(), "customerCancelsOrder");
        rule.starting(desc2);
        rule.context().enterMethod(new MethodSignature("OrderService", "cancelOrder", List.of()));
        rule.context().exitMethodWithReturn("cancelled");
        rule.finished(desc2);

        assertThat(classRule.accumulatedTraces()).hasSize(2);
        assertThat(classRule.accumulatedTraces()).containsKeys("Customer places order", "Customer cancels order");
    }

    @Test
    void applWritesClarityReportAndConsoleSummary(@TempDir Path tempDir) throws Throwable {
        System.setProperty("narrativetrace.output", "true");
        System.setProperty("narrativetrace.outputDir", tempDir.toString());
        try {
            var classRule = new NarrativeTraceClassRule();
            var captured = new ByteArrayOutputStream();
            classRule.setOut(new PrintStream(captured));
            var rule = classRule.testRule();
            rule.setOut(new PrintStream(new ByteArrayOutputStream()));
            rule.setMermaidRenderer(tree -> "sequenceDiagram");
            rule.setPlantumlRenderer(tree -> "@startuml\n@enduml");

            var statement = classRule.apply(new Statement() {
                @Override
                public void evaluate() {
                    var desc1 = Description.createTestDescription("com.example.Test", "customerPlacesOrder");
                    rule.starting(desc1);
                    rule.context().enterMethod(new MethodSignature("OrderService", "placeOrder", List.of()));
                    rule.context().exitMethodWithReturn("order-1");
                    rule.finished(desc1);

                    var desc2 = Description.createTestDescription("com.example.Test", "customerCancelsOrder");
                    rule.starting(desc2);
                    rule.context().enterMethod(new MethodSignature("OrderService", "cancelOrder", List.of()));
                    rule.context().exitMethodWithReturn("cancelled");
                    rule.finished(desc2);
                }
            }, Description.createSuiteDescription(getClass()));

            statement.evaluate();

            var reportFile = tempDir.resolve("clarity-report.md");
            assertThat(reportFile).exists();
            var reportContent = Files.readString(reportFile);
            assertThat(reportContent).contains("Customer places order");
            assertThat(reportContent).contains("Customer cancels order");

            var consoleOutput = captured.toString();
            assertThat(consoleOutput).contains("NarrativeTrace — Suite complete");
            assertThat(consoleOutput).contains("2 scenarios recorded");
        } finally {
            System.clearProperty("narrativetrace.output");
            System.clearProperty("narrativetrace.outputDir");
        }
    }

    @Test
    void applyWritesClarityJson(@TempDir Path tempDir) throws Throwable {
        System.setProperty("narrativetrace.output", "true");
        System.setProperty("narrativetrace.outputDir", tempDir.toString());
        try {
            var classRule = new NarrativeTraceClassRule();
            classRule.setOut(new PrintStream(new ByteArrayOutputStream()));

            classRule.accumulate("Customer places order", traceWithOneCall());
            var statement = classRule.apply(new Statement() {
                @Override
                public void evaluate() {}
            }, Description.createSuiteDescription(getClass()));
            statement.evaluate();

            var jsonFile = tempDir.resolve("clarity-results.json");
            assertThat(jsonFile).exists();
            var content = Files.readString(jsonFile);
            assertThat(content).contains("\"version\":\"1.0\"");
            assertThat(content).contains("\"scenarios\":");
        } finally {
            System.clearProperty("narrativetrace.output");
            System.clearProperty("narrativetrace.outputDir");
        }
    }

    @Test
    void applySkipsReportWhenOutputDisabled() throws Throwable {
        System.clearProperty("narrativetrace.output");
        var classRule = new NarrativeTraceClassRule();
        var captured = new ByteArrayOutputStream();
        classRule.setOut(new PrintStream(captured));

        classRule.accumulate("test", traceWithOneCall());
        var statement = classRule.apply(new Statement() {
            @Override
            public void evaluate() {}
        }, Description.createSuiteDescription(getClass()));
        statement.evaluate();

        assertThat(captured.toString()).isEmpty();
    }

    @Test
    void applyCatchesIoErrorOnClarityReportWrite(@TempDir Path tempDir) throws Throwable {
        Files.createDirectories(tempDir.resolve("clarity-report.md"));
        System.setProperty("narrativetrace.output", "true");
        System.setProperty("narrativetrace.outputDir", tempDir.toString());
        try {
            var classRule = new NarrativeTraceClassRule();
            var errStream = new ByteArrayOutputStream();
            classRule.setOut(new PrintStream(new ByteArrayOutputStream()));
            classRule.setErr(new PrintStream(errStream));

            classRule.accumulate("test", traceWithOneCall());
            var statement = classRule.apply(new Statement() {
                @Override
                public void evaluate() {}
            }, Description.createSuiteDescription(getClass()));
            statement.evaluate();

            assertThat(errStream.toString()).contains("Failed to write clarity report:");
        } finally {
            System.clearProperty("narrativetrace.output");
            System.clearProperty("narrativetrace.outputDir");
        }
    }

    @Test
    void applySkipsReportWhenNoTracesAccumulated(@TempDir Path tempDir) throws Throwable {
        System.setProperty("narrativetrace.output", "true");
        System.setProperty("narrativetrace.outputDir", tempDir.toString());
        try {
            var classRule = new NarrativeTraceClassRule();
            var captured = new ByteArrayOutputStream();
            classRule.setOut(new PrintStream(captured));
            var statement = classRule.apply(new Statement() {
                @Override
                public void evaluate() {}
            }, Description.createSuiteDescription(getClass()));
            statement.evaluate();

            assertThat(captured.toString()).isEmpty();
            assertThat(tempDir.resolve("clarity-report.md")).doesNotExist();
        } finally {
            System.clearProperty("narrativetrace.output");
            System.clearProperty("narrativetrace.outputDir");
        }
    }

    @Test
    void standaloneRuleDoesNotAccumulate() {
        var rule = new NarrativeTraceRule();

        var desc = Description.createTestDescription(getClass(), "test");
        rule.starting(desc);
        rule.context().enterMethod(new MethodSignature("Service", "doWork", List.of()));
        rule.context().exitMethodWithReturn("ok");
        rule.finished(desc);

        // no exception, no accumulation (no class rule linked)
    }

    @Test
    void combinedReportContainsScenariosFromMultipleClasses(@TempDir Path tempDir) throws Throwable {
        System.setProperty("narrativetrace.output", "true");
        System.setProperty("narrativetrace.outputDir", tempDir.toString());
        try {
            // Simulate first class
            var classRule1 = new NarrativeTraceClassRule();
            classRule1.setOut(new PrintStream(new ByteArrayOutputStream()));
            classRule1.accumulate("Customer places order", traceWithOneCall());
            var stmt1 = classRule1.apply(new Statement() {
                @Override
                public void evaluate() {}
            }, Description.createSuiteDescription("FirstClass"));
            stmt1.evaluate();

            // Simulate second class
            var classRule2 = new NarrativeTraceClassRule();
            classRule2.setOut(new PrintStream(new ByteArrayOutputStream()));
            classRule2.accumulate("Customer checks inventory", traceWithOneCall());
            var stmt2 = classRule2.apply(new Statement() {
                @Override
                public void evaluate() {}
            }, Description.createSuiteDescription("SecondClass"));
            stmt2.evaluate();

            var jsonFile = tempDir.resolve("clarity-results.json");
            assertThat(jsonFile).exists();
            var content = Files.readString(jsonFile);
            assertThat(content).contains("Customer places order");
            assertThat(content).contains("Customer checks inventory");

            var reportFile = tempDir.resolve("clarity-report.md");
            assertThat(reportFile).exists();
            var reportContent = Files.readString(reportFile);
            assertThat(reportContent).contains("Customer places order");
            assertThat(reportContent).contains("Customer checks inventory");
        } finally {
            System.clearProperty("narrativetrace.output");
            System.clearProperty("narrativetrace.outputDir");
        }
    }

    private static TraceTree traceWithOneCall() {
        var node = new TraceNode(
                new MethodSignature("Service", "doWork", List.of()),
                List.of(),
                new TraceOutcome.Returned("\"ok\""));
        return new DefaultTraceTree(List.of(node));
    }
}
