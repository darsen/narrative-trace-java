package ai.narrativetrace.examples.clarity;

public class DefaultPaymentGateway implements PaymentGateway {

  @Override
  public boolean authorizePayment(String reservationId, double amount) {
    return true;
  }
}
