package ai.narrativetrace.junit5;

import ai.narrativetrace.core.context.ThreadLocalNarrativeContext;
import ai.narrativetrace.core.event.MethodSignature;
import ai.narrativetrace.core.event.ParameterCapture;
import ai.narrativetrace.core.output.ScenarioFramer;
import ai.narrativetrace.core.output.TraceTestSupport;
import ai.narrativetrace.core.render.IndentedTextRenderer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FailureReportTest {

    @Test
    void buildsFailureReportCombiningScenarioHeaderAndTrace() {
        var context = new ThreadLocalNarrativeContext();
        context.enterMethod(new MethodSignature("OrderService", "placeOrder",
                List.of(new ParameterCapture("customerId", "\"C-123\"", false))));
        context.exitMethodWithReturn("\"order-42\"");

        var scenario = ScenarioFramer.frame("placing an order succeeds");
        var trace = new IndentedTextRenderer().render(context.captureTrace());
        var report = TraceTestSupport.buildFailureReport(scenario, trace);

        assertThat(report).contains("Scenario: placing an order succeeds");
        assertThat(report).contains("Execution trace:");
        assertThat(report).contains("OrderService.placeOrder(customerId: \"C-123\") → \"order-42\"");
    }
}
