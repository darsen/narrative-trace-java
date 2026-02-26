package ai.narrativetrace.examples.ecommerce;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class JsonPlaceholderNotificationService implements NotificationService {

    private final HttpClient client = HttpClient.newHttpClient();

    @Override
    public CompletableFuture<Boolean> notifyOrderPlaced(String customerId, String orderId) {
        try {
            var body = """
                    {"customerId": "%s", "orderId": "%s", "type": "ORDER_CONFIRMATION"}"""
                    .formatted(customerId, orderId);

            var request = HttpRequest.newBuilder()
                    .uri(URI.create("https://jsonplaceholder.typicode.com/posts"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return CompletableFuture.completedFuture(response.statusCode() == 201);
        } catch (Exception e) {
            throw new ExternalServiceException("Failed to send notification for order " + orderId, e);
        }
    }
}
