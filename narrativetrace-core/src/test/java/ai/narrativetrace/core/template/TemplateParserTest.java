package ai.narrativetrace.core.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class TemplateParserTest {

  @Test
  void resolvesSimpleParameterSubstitution() {
    var result =
        TemplateParser.resolve(
            "Placing order of {quantity} units for customer {customerId}",
            Map.of("quantity", 5, "customerId", "C-123"));

    assertThat(result).isEqualTo("Placing order of 5 units for customer C-123");
  }

  record Customer(String name, String tier) {}

  @Test
  void resolvesOneLevelPropertyAccess() {
    var result =
        TemplateParser.resolve(
            "Order for {customer.name} (tier: {customer.tier})",
            Map.of("customer", new Customer("Alice", "GOLD")));

    assertThat(result).isEqualTo("Order for Alice (tier: GOLD)");
  }

  @Test
  void leavesMissingParamAsIs() {
    var result =
        TemplateParser.resolve(
            "Order for {customerId} with {unknown}", Map.of("customerId", "C-123"));

    assertThat(result).isEqualTo("Order for C-123 with {unknown}");
  }

  @Test
  void missingObjectInDottedPlaceholderPreservesPlaceholder() {
    var result = TemplateParser.resolve("Hello {missing.name}", Map.of());

    assertThat(result).isEqualTo("Hello {missing.name}");
  }

  @Test
  void invalidPropertyFallsBackToPlaceholder() {
    var result =
        TemplateParser.resolve(
            "Value: {customer.nonexistent}", Map.of("customer", new Customer("Alice", "GOLD")));

    assertThat(result).isEqualTo("Value: {customer.nonexistent}");
  }

  @Test
  void nullPropertyValuePreservesPlaceholder() {
    var result =
        TemplateParser.resolve("Value: {customer.name}", Map.of("customer", new NullName()));

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

  @Test
  void propertyAccessorThrowingUncheckedExceptionPreservesPlaceholder() {
    var result =
        TemplateParser.resolve("Value: {obj.explode}", Map.of("obj", new ThrowsOnAccess()));

    assertThat(result).isEqualTo("Value: {obj.explode}");
  }

  @Test
  void parseProducesDottedPropertyPlaceholder() {
    var segments = TemplateParser.parse("{order.total}");

    assertThat(segments).hasSize(1);
    assertThat(segments.get(0)).isInstanceOf(TemplateParser.Segment.PropertyPlaceholder.class);
    var prop = (TemplateParser.Segment.PropertyPlaceholder) segments.get(0);
    assertThat(prop.objectKey()).isEqualTo("order");
    assertThat(prop.property()).isEqualTo("total");
  }

  @Test
  void parseProducesSimplePlaceholderForUndottedKey() {
    var segments = TemplateParser.parse("{name}");

    assertThat(segments).hasSize(1);
    assertThat(segments.get(0)).isInstanceOf(TemplateParser.Segment.SimplePlaceholder.class);
    var simple = (TemplateParser.Segment.SimplePlaceholder) segments.get(0);
    assertThat(simple.key()).isEqualTo("name");
  }

  @Test
  void parsePreservesLiteralsBetweenAdjacentPlaceholders() {
    var segments = TemplateParser.parse("a{x}b{y}c");

    assertThat(segments).hasSize(5);
    assertThat(segments.get(0)).isInstanceOf(TemplateParser.Segment.Literal.class);
    assertThat(segments.get(1)).isInstanceOf(TemplateParser.Segment.SimplePlaceholder.class);
    assertThat(segments.get(2)).isInstanceOf(TemplateParser.Segment.Literal.class);
    assertThat(((TemplateParser.Segment.Literal) segments.get(2)).text()).isEqualTo("b");
    assertThat(segments.get(3)).isInstanceOf(TemplateParser.Segment.SimplePlaceholder.class);
    assertThat(segments.get(4)).isInstanceOf(TemplateParser.Segment.Literal.class);
    assertThat(((TemplateParser.Segment.Literal) segments.get(4)).text()).isEqualTo("c");
  }

  @Test
  void parseHandlesAdjacentPlaceholdersWithNoLiteralBetween() {
    var segments = TemplateParser.parse("{a}{b}");

    assertThat(segments).hasSize(2);
    assertThat(segments.get(0)).isInstanceOf(TemplateParser.Segment.SimplePlaceholder.class);
    assertThat(segments.get(1)).isInstanceOf(TemplateParser.Segment.SimplePlaceholder.class);
  }

  @Test
  void parsePreservesTrailingLiteral() {
    var segments = TemplateParser.parse("{x} end");

    assertThat(segments).hasSize(2);
    assertThat(segments.get(0)).isInstanceOf(TemplateParser.Segment.SimplePlaceholder.class);
    assertThat(segments.get(1)).isInstanceOf(TemplateParser.Segment.Literal.class);
    assertThat(((TemplateParser.Segment.Literal) segments.get(1)).text()).isEqualTo(" end");
  }

  static class NullName {
    public String name() {
      return null;
    }
  }

  static class ThrowsOnAccess {
    public String explode() {
      throw new RuntimeException("boom");
    }
  }
}
