package ai.narrativetrace.examples.ecommerce;

import ai.narrativetrace.core.annotation.Narrated;

public interface OrderService {
  @Narrated("Placing order of {quantity} {productId} for customer {customerId}")
  OrderResult placeOrder(String customerId, String productId, int quantity);
}
