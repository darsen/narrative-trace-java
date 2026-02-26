package ai.narrativetrace.examples.ecommerce;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

class ECommerceExampleTest {

    @Test
    void mainRunsAllScenariosWithoutError() {
        var out = new ByteArrayOutputStream();
        var original = System.out;
        System.setOut(new PrintStream(out));
        try {
            ECommerceExample.main(new String[]{});
        } finally {
            System.setOut(original);
        }

        var output = out.toString();
        assertThat(output).contains("Scenario 1: Successful Order");
        assertThat(output).contains("Scenario 2: Payment Failure");
        assertThat(output).contains("Scenario 3: Flaky External Service");
        assertThat(output).contains("Scenario 4: Unknown Customer");
        assertThat(output).contains("Scenario 5: Out of Stock");
        assertThat(output).contains("InventoryService.reserve");
        assertThat(output).contains("sequenceDiagram");
    }

    @Test
    void classCanBeInstantiated() {
        var example = new ECommerceExample();
        assertThat(example).isNotNull();
    }
}
