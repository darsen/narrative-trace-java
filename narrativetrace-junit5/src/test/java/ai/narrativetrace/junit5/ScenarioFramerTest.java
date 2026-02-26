package ai.narrativetrace.junit5;

import ai.narrativetrace.core.output.ScenarioFramer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScenarioFramerTest {

    @Test
    void formatsDisplayNameAsScenarioHeader() {
        var result = ScenarioFramer.frame("placeOrder creates order for valid customer");

        assertThat(result).isEqualTo("Scenario: placeOrder creates order for valid customer");
    }

    @Test
    void humanizesCamelCaseMethodName() {
        var result = ScenarioFramer.frame("customerPlacesOrder()");

        assertThat(result).isEqualTo("Scenario: Customer places order");
    }

    @Test
    void preservesCustomDisplayName() {
        var result = ScenarioFramer.frame("Customer places order with loyalty discount");

        assertThat(result).isEqualTo("Scenario: Customer places order with loyalty discount");
    }

    @Test
    void humanizesUnderscoreSeparatedMethodName() {
        var result = ScenarioFramer.frame("customer_places_order_with_expired_card()");

        assertThat(result).isEqualTo("Scenario: Customer places order with expired card");
    }

    @Test
    void stripsJunitParameterTypeSuffix() {
        var result = ScenarioFramer.frame("customerPlacesOrder(NarrativeContext)");

        assertThat(result).isEqualTo("Scenario: Customer places order");
    }

    @Test
    void stripsParameterSuffixFromCustomDisplayName() {
        var result = ScenarioFramer.humanize("customer places order(narrative context)");

        assertThat(result).isEqualTo("customer places order");
    }
}
