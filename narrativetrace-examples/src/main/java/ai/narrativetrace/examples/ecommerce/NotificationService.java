package ai.narrativetrace.examples.ecommerce;

import ai.narrativetrace.core.annotation.OnError;
import java.util.concurrent.CompletableFuture;
import org.springframework.scheduling.annotation.Async;

public interface NotificationService {
  @Async
  @OnError(
      value = "Failed to notify customer {customerId} about order {orderId}",
      exception = ExternalServiceException.class)
  CompletableFuture<Boolean> notifyOrderPlaced(String customerId, String orderId);
}
