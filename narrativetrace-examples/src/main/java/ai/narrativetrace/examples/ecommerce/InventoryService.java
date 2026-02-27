package ai.narrativetrace.examples.ecommerce;

import ai.narrativetrace.core.annotation.OnError;

public interface InventoryService {
  @OnError(
      value = "Insufficient stock for {productId}, requested {quantity}",
      exception = IllegalStateException.class)
  Reservation reserve(String productId, int quantity);

  void release(String productId, int quantity);
}
