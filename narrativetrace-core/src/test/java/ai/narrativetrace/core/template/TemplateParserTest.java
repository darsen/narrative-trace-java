package ai.narrativetrace.core.template;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateParserTest {

    @Test
    void resolvesSimpleParameterSubstitution() {
        var result = TemplateParser.resolve(
                "Placing order of {quantity} units for customer {customerId}",
                Map.of("quantity", 5, "customerId", "C-123"));

        assertThat(result).isEqualTo("Placing order of 5 units for customer C-123");
    }

    record Customer(String name, String tier) {}

    @Test
    void resolvesOneLevelPropertyAccess() {
        var result = TemplateParser.resolve(
                "Order for {customer.name} (tier: {customer.tier})",
                Map.of("customer", new Customer("Alice", "GOLD")));

        assertThat(result).isEqualTo("Order for Alice (tier: GOLD)");
    }

    @Test
    void leavesMissingParamAsIs() {
        var result = TemplateParser.resolve(
                "Order for {customerId} with {unknown}",
                Map.of("customerId", "C-123"));

        assertThat(result).isEqualTo("Order for C-123 with {unknown}");
    }

    @Test
    void missingObjectInDottedPlaceholderPreservesPlaceholder() {
        var result = TemplateParser.resolve(
                "Hello {missing.name}",
                Map.of());

        assertThat(result).isEqualTo("Hello {missing.name}");
    }

    @Test
    void invalidPropertyFallsBackToPlaceholder() {
        var result = TemplateParser.resolve(
                "Value: {customer.nonexistent}",
                Map.of("customer", new Customer("Alice", "GOLD")));

        assertThat(result).isEqualTo("Value: {customer.nonexistent}");
    }

    @Test
    void nullPropertyValuePreservesPlaceholder() {
        var result = TemplateParser.resolve(
                "Value: {customer.name}",
                Map.of("customer", new NullName()));

        assertThat(result).isEqualTo("Value: {customer.name}");
    }

    @Test
    void sameTemplateWithDifferentValuesProducesCorrectResults() {
        var template = "{item} costs {price}";

        var first = TemplateParser.resolve(template, Map.of("item", "Widget", "price", 9.99));
        var second = TemplateParser.resolve(template, Map.of("item", "Gadget", "price", 19.99));

        assertThat(first).isEqualTo("Widget costs 9.99");
        assertThat(second).isEqualTo("Gadget costs 19.99");
    }

    @Test
    void findUnresolvedInResultReturnsEmptyForNull() {
        assertThat(TemplateParser.findUnresolvedInResult(null)).isEmpty();
    }

    @Test
    void findUnresolvedInResultReturnsEmptyWhenNoPlaceholders() {
        assertThat(TemplateParser.findUnresolvedInResult("Order for Alice")).isEmpty();
    }

    @Test
    void findUnresolvedInResultFindsSurvivingPlaceholders() {
        assertThat(TemplateParser.findUnresolvedInResult("Order for {custmerId} with {order.stauts}"))
                .containsExactly("custmerId", "order.stauts");
    }

    static class NullName {
        public String name() { return null; }
    }
}
