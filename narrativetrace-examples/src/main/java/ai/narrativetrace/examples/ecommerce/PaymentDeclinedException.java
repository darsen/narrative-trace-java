package ai.narrativetrace.examples.ecommerce;

public class PaymentDeclinedException extends RuntimeException {
    public PaymentDeclinedException(String message) {
        super(message);
    }
}
