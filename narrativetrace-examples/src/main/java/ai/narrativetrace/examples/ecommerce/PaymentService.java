package ai.narrativetrace.examples.ecommerce;

import ai.narrativetrace.core.annotation.NotTraced;
import ai.narrativetrace.core.annotation.OnError;

public interface PaymentService {
  @OnError(
      value = "Payment declined for customer {customerId}, amount was {amount}",
      exception = PaymentDeclinedException.class)
  PaymentConfirmation charge(String customerId, double amount, @NotTraced String cardToken);
}
