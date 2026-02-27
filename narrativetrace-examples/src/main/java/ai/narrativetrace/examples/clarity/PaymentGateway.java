package ai.narrativetrace.examples.clarity;

public interface PaymentGateway {
  boolean authorizePayment(String reservationId, double amount);
}
