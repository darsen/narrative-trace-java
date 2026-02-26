package ai.narrativetrace.examples.ecommerce;

public record OrderResult(String orderId, String transactionId, double totalCharged, int itemCount) {
}
