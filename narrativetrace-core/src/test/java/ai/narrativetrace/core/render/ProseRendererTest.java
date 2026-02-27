package ai.narrativetrace.core.render;

import static org.assertj.core.api.Assertions.assertThat;

import ai.narrativetrace.core.event.MethodSignature;
import ai.narrativetrace.core.event.ParameterCapture;
import ai.narrativetrace.core.event.TraceNode;
import ai.narrativetrace.core.event.TraceOutcome;
import ai.narrativetrace.core.tree.DefaultTraceTree;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProseRendererTest {

  private final ProseRenderer renderer = new ProseRenderer();

  @Test
  void renders_simple_leaf_with_return() {
    var node =
        new TraceNode(
            new MethodSignature(
                "OrderService",
                "placeOrder",
                List.of(new ParameterCapture("customerId", "\"C-123\"", false))),
            List.of(),
            new TraceOutcome.Returned("\"ok\""));
    var tree = new DefaultTraceTree(List.of(node));

    var result = renderer.render(tree);

    assertThat(result)
        .isEqualTo("The order service place order for customerId: \"C-123\", returning \"ok\".");
  }

  @Test
  void renders_leaf_with_multiple_params() {
    var node =
        new TraceNode(
            new MethodSignature(
                "PaymentGateway",
                "charge",
                List.of(
                    new ParameterCapture("customerId", "\"C-123\"", false),
                    new ParameterCapture("amount", "242.95", false))),
            List.of(),
            new TraceOutcome.Returned("\"ok\""));
    var tree = new DefaultTraceTree(List.of(node));

    var result = renderer.render(tree);

    assertThat(result)
        .isEqualTo(
            "The payment gateway charge for customerId: \"C-123\" amount: 242.95, returning \"ok\".");
  }

  @Test
  void renders_leaf_with_no_params() {
    var node =
        new TraceNode(
            new MethodSignature("OrderService", "placeOrder", List.of()),
            List.of(),
            new TraceOutcome.Returned("\"ok\""));
    var tree = new DefaultTraceTree(List.of(node));

    var result = renderer.render(tree);

    assertThat(result).isEqualTo("The order service place order, returning \"ok\".");
  }

  @Test
  void renders_redacted_params() {
    var node =
        new TraceNode(
            new MethodSignature(
                "AuthService",
                "login",
                List.of(
                    new ParameterCapture("username", "\"admin\"", false),
                    new ParameterCapture("password", "[REDACTED]", true))),
            List.of(),
            new TraceOutcome.Returned("true"));
    var tree = new DefaultTraceTree(List.of(node));

    var result = renderer.render(tree);

    assertThat(result)
        .isEqualTo(
            "The auth service login for username: \"admin\" password: [REDACTED], returning true.");
  }

  @Test
  void renders_complex_return_value() {
    var node =
        new TraceNode(
            new MethodSignature("PricingEngine", "calculateTotal", List.of()),
            List.of(),
            new TraceOutcome.Returned("[\"item1\", \"item2\"]"));
    var tree = new DefaultTraceTree(List.of(node));

    var result = renderer.render(tree);

    assertThat(result)
        .isEqualTo("The pricing engine calculate total, returning [\"item1\", \"item2\"].");
  }

  @Test
  void renders_narration_when_present() {
    var node =
        new TraceNode(
            new MethodSignature(
                "OrderService",
                "placeOrder",
                List.of(new ParameterCapture("customerId", "\"C-123\"", false)),
                "Placing order of 5 units for customer C-123",
                null),
            List.of(),
            new TraceOutcome.Returned("\"ok\""));
    var tree = new DefaultTraceTree(List.of(node));

    var result = renderer.render(tree);

    assertThat(result)
        .isEqualTo(
            "The order service place order — Placing order of 5 units for customer C-123, returning \"ok\".");
  }

  @Test
  void renders_narration_with_children() {
    var child =
        new TraceNode(
            new MethodSignature(
                "InventoryService",
                "checkStock",
                List.of(new ParameterCapture("itemId", "\"ITEM-1\"", false))),
            List.of(),
            new TraceOutcome.Returned("true"));
    var parent =
        new TraceNode(
            new MethodSignature(
                "OrderService",
                "placeOrder",
                List.of(new ParameterCapture("customerId", "\"C-123\"", false)),
                "Placing order of 5 units for customer C-123",
                null),
            List.of(child),
            new TraceOutcome.Returned("\"ok\""));
    var tree = new DefaultTraceTree(List.of(parent));

    var result = renderer.render(tree);

    assertThat(result)
        .isEqualTo(
            """
                The order service place order — Placing order of 5 units for customer C-123:
                  The inventory service check stock for itemId: "ITEM-1", returning true.
                  Returned "ok".""");
  }

  @Test
  void renders_parent_with_children() {
    var child1 =
        new TraceNode(
            new MethodSignature(
                "OrderValidator",
                "validateCart",
                List.of(new ParameterCapture("cartId", "\"CART-77\"", false))),
            List.of(),
            new TraceOutcome.Returned("true"));
    var child2 =
        new TraceNode(
            new MethodSignature(
                "PaymentGateway",
                "charge",
                List.of(new ParameterCapture("amount", "242.95", false))),
            List.of(),
            new TraceOutcome.Returned("\"ok\""));
    var parent =
        new TraceNode(
            new MethodSignature("OrderController", "submitOrder", List.of()),
            List.of(child1, child2),
            new TraceOutcome.Returned("\"done\""));
    var tree = new DefaultTraceTree(List.of(parent));

    var result = renderer.render(tree);

    assertThat(result)
        .isEqualTo(
            """
                The order controller submit order:
                  The order validator validate cart for cartId: "CART-77", returning true.
                  The payment gateway charge for amount: 242.95, returning "ok".
                  Returned "done".""");
  }

  @Test
  void renders_nested_tree() {
    var grandchild =
        new TraceNode(
            new MethodSignature(
                "DiscountService",
                "findDiscounts",
                List.of(new ParameterCapture("customerId", "\"C-123\"", false))),
            List.of(),
            new TraceOutcome.Returned("\"10%\""));
    var child =
        new TraceNode(
            new MethodSignature(
                "PricingEngine",
                "calculateTotal",
                List.of(new ParameterCapture("cartId", "\"CART-77\"", false))),
            List.of(grandchild),
            new TraceOutcome.Returned("242.95"));
    var parent =
        new TraceNode(
            new MethodSignature("OrderController", "submitOrder", List.of()),
            List.of(child),
            new TraceOutcome.Returned("\"done\""));
    var tree = new DefaultTraceTree(List.of(parent));

    var result = renderer.render(tree);

    assertThat(result)
        .isEqualTo(
            """
                The order controller submit order:
                  The pricing engine calculate total for cartId: "CART-77":
                    The discount service find discounts for customerId: "C-123", returning "10%".
                    Returned 242.95.
                  Returned "done".""");
  }

  @Test
  void renders_multiple_roots() {
    var root1 =
        new TraceNode(
            new MethodSignature("OrderService", "placeOrder", List.of()),
            List.of(),
            new TraceOutcome.Returned("\"ok\""));
    var root2 =
        new TraceNode(
            new MethodSignature("NotificationService", "sendEmail", List.of()),
            List.of(),
            new TraceOutcome.Returned(null));
    var tree = new DefaultTraceTree(List.of(root1, root2));

    var result = renderer.render(tree);

    assertThat(result)
        .isEqualTo(
            """
                The order service place order, returning "ok".
                The notification service send email.""");
  }

  @Test
  void renders_error_leaf() {
    var node =
        new TraceNode(
            new MethodSignature(
                "PaymentGateway",
                "charge",
                List.of(
                    new ParameterCapture("customerId", "\"C-456\"", false),
                    new ParameterCapture("amount", "97.14", false))),
            List.of(),
            new TraceOutcome.Threw(new IllegalStateException("Card expired")));
    var tree = new DefaultTraceTree(List.of(node));

    var result = renderer.render(tree);

    assertThat(result)
        .isEqualTo(
            "The payment gateway failed to charge for customerId: \"C-456\" amount: 97.14 — IllegalStateException: Card expired.");
  }

  @Test
  void renders_error_with_error_context() {
    var node =
        new TraceNode(
            new MethodSignature(
                "PaymentGateway",
                "charge",
                List.of(new ParameterCapture("amount", "99.95", false)),
                null,
                "Payment failed for amount 99.95"),
            List.of(),
            new TraceOutcome.Threw(new IllegalStateException("Card expired")));
    var tree = new DefaultTraceTree(List.of(node));

    var result = renderer.render(tree);

    assertThat(result)
        .isEqualTo(
            "The payment gateway failed to charge for amount: 99.95 — IllegalStateException: Card expired (Payment failed for amount 99.95).");
  }

  @Test
  void renders_parent_with_threw_outcome_in_closing() {
    var child =
        new TraceNode(
            new MethodSignature("PaymentService", "charge", List.of()),
            List.of(),
            new TraceOutcome.Threw(new RuntimeException("declined")));
    var parent =
        new TraceNode(
            new MethodSignature("OrderService", "placeOrder", List.of()),
            List.of(child),
            new TraceOutcome.Threw(new RuntimeException("order failed")));
    var tree = new DefaultTraceTree(List.of(parent));

    var result = renderer.render(tree);

    assertThat(result).contains("RuntimeException: order failed.");
  }

  @Test
  void renders_leaf_with_void_return() {
    var node =
        new TraceNode(
            new MethodSignature(
                "OrderService",
                "placeOrder",
                List.of(new ParameterCapture("customerId", "\"C-123\"", false))),
            List.of(),
            new TraceOutcome.Returned(null));
    var tree = new DefaultTraceTree(List.of(node));

    var result = renderer.render(tree);

    assertThat(result).isEqualTo("The order service place order for customerId: \"C-123\".");
  }
}
