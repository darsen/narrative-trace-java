package ai.narrativetrace.junit4;

import ai.narrativetrace.core.context.ThreadLocalNarrativeContext;
import ai.narrativetrace.core.event.MethodSignature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.runner.Description;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NarrativeTraceRuleTest {

    @Test
    void contextIsNullBeforeStarting() {
        var rule = new NarrativeTraceRule();

        assertThat(rule.context()).isNull();
    }

    @Test
    void startingCreatesThreadLocalContext() {
        var rule = new NarrativeTraceRule();
        rule.starting(Description.createTestDescription(getClass(), "test"));

        assertThat(rule.context()).isNotNull();
        assertThat(rule.context()).isInstanceOf(ThreadLocalNarrativeContext.class);
    }

    @Test
    void eachStartingCreatesNewContext() {
        var rule = new NarrativeTraceRule();
        rule.starting(Description.createTestDescription(getClass(), "test1"));
        var first = rule.context();
        rule.starting(Description.createTestDescription(getClass(), "test2"));

        assertThat(rule.context()).isNotSameAs(first);
    }

    @Test
    void failedPrintsFailureReportWhenTraceIsNonEmpty() {
        var rule = new NarrativeTraceRule();
        var out = new ByteArrayOutputStream();
        rule.setOut(new PrintStream(out));
        var desc = Description.createTestDescription(getClass(), "customerPlacesOrder");
        rule.starting(desc);
        rule.context().enterMethod(new MethodSignature("Service", "doWork", List.of()));
        rule.context().exitMethodWithReturn("ok");

        rule.failed(new AssertionError("expected"), desc);

        var output = out.toString();
        assertThat(output).contains("Scenario: Customer places order");
        assertThat(output).contains("Service.doWork");
    }

    @Test
    void failedDoesNothingWhenTraceIsEmpty() {
        var rule = new NarrativeTraceRule();
        var out = new ByteArrayOutputStream();
        rule.setOut(new PrintStream(out));
        var desc = Description.createTestDescription(getClass(), "test");
        rule.starting(desc);

        rule.failed(new AssertionError("expected"), desc);

        assertThat(out.toString()).isEmpty();
    }

    @Test
    void finishedWritesTraceFileWhenOutputEnabled(@TempDir Path tempDir) throws Exception {
        System.setProperty("narrativetrace.output", "true");
        System.setProperty("narrativetrace.outputDir", tempDir.toString());
        try {
            var rule = new NarrativeTraceRule();
            var out = new ByteArrayOutputStream();
            rule.setOut(new PrintStream(out));
            rule.setMermaidRenderer(tree -> "sequenceDiagram");
            rule.setPlantumlRenderer(tree -> "@startuml\n@enduml");
            var desc = Description.createTestDescription("com.example.OrderTest", "customerPlacesOrder");
            rule.starting(desc);
            rule.context().enterMethod(new MethodSignature("Service", "doWork", List.of()));
            rule.context().exitMethodWithReturn("ok");

            rule.finished(desc);

            var file = tempDir.resolve("traces/OrderTest/customer_places_order.md");
            assertThat(file).exists();
            assertThat(Files.readString(file)).contains("Service.doWork");
        } finally {
            System.clearProperty("narrativetrace.output");
            System.clearProperty("narrativetrace.outputDir");
        }
    }

    @Test
    void finishedDoesNotWriteWhenOutputDisabled() {
        System.clearProperty("narrativetrace.output");
        var rule = new NarrativeTraceRule();
        var out = new ByteArrayOutputStream();
        rule.setOut(new PrintStream(out));
        var desc = Description.createTestDescription("com.example.OrderTest", "test");
        rule.starting(desc);
        rule.context().enterMethod(new MethodSignature("Service", "doWork", List.of()));
        rule.context().exitMethodWithReturn("ok");

        rule.finished(desc);

        assertThat(out.toString()).isEmpty();
    }

    @Test
    void finishedSafeWhenContextIsNull() {
        var rule = new NarrativeTraceRule();
        var desc = Description.createTestDescription(getClass(), "test");

        rule.finished(desc);
        // no exception
    }

    @Test
    void writeTraceFileCatchesIoError(@TempDir Path tempDir) throws Exception {
        // Create a file where the traces directory should be, forcing IOException
        Files.writeString(tempDir.resolve("traces"), "blocker");
        System.setProperty("narrativetrace.output", "true");
        System.setProperty("narrativetrace.outputDir", tempDir.toString());
        try {
            var rule = new NarrativeTraceRule();
            var out = new ByteArrayOutputStream();
            var errStream = new ByteArrayOutputStream();
            rule.setOut(new PrintStream(out));
            rule.setErr(new PrintStream(errStream));
            var desc = Description.createTestDescription("com.example.Test", "test");
            rule.starting(desc);
            rule.context().enterMethod(new MethodSignature("Service", "doWork", List.of()));
            rule.context().exitMethodWithReturn("ok");

            rule.finished(desc);

            assertThat(errStream.toString()).contains("Failed to write trace file:");
        } finally {
            System.clearProperty("narrativetrace.output");
            System.clearProperty("narrativetrace.outputDir");
        }
    }

    @Test
    void finishedPrintsTemplateWarningsWhenUnresolvedPlaceholders() {
        var rule = new NarrativeTraceRule();
        var out = new ByteArrayOutputStream();
        rule.setOut(new PrintStream(out));
        var desc = Description.createTestDescription(getClass(), "placeOrder");
        rule.starting(desc);
        rule.context().enterMethod(new MethodSignature("OrderService", "placeOrder",
                List.of(), "Placing order for {custmerId}", null));
        rule.context().exitMethodWithReturn("ok");

        rule.finished(desc);

        var output = out.toString();
        assertThat(output).contains("Unresolved template placeholder");
        assertThat(output).contains("OrderService.placeOrder");
        assertThat(output).contains("{custmerId}");
    }

    @Test
    void finishedNoWarningsForCleanTrace() {
        var rule = new NarrativeTraceRule();
        var out = new ByteArrayOutputStream();
        rule.setOut(new PrintStream(out));
        var desc = Description.createTestDescription(getClass(), "placeOrder");
        rule.starting(desc);
        rule.context().enterMethod(new MethodSignature("OrderService", "placeOrder",
                List.of(), "Placing order for C-123", null));
        rule.context().exitMethodWithReturn("ok");

        rule.finished(desc);

        assertThat(out.toString()).isEmpty();
    }

    @Test
    void isOutputEnabledReadsSysProp() {
        System.setProperty("narrativetrace.output", "true");
        try {
            assertThat(NarrativeTraceRule.isOutputEnabled()).isTrue();
        } finally {
            System.clearProperty("narrativetrace.output");
        }
        assertThat(NarrativeTraceRule.isOutputEnabled()).isFalse();
    }
}
