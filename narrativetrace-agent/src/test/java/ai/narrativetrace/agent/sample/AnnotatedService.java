package ai.narrativetrace.agent.sample;

import ai.narrativetrace.core.annotation.Narrated;
import ai.narrativetrace.core.annotation.NotTraced;
import ai.narrativetrace.core.annotation.OnError;

public class AnnotatedService {

    public String login(String username, @NotTraced String password) {
        return "token-for-" + username;
    }

    @Narrated("Processing {item} for quantity {quantity}")
    public double calculatePrice(String item, int quantity) {
        return quantity * 9.99;
    }

    @OnError(value = "Transfer failed for amount {amount}", exception = IllegalArgumentException.class)
    @OnError(value = "Transfer error for amount {amount}", exception = Throwable.class)
    public void transfer(String from, String to, int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Negative amount: " + amount);
        }
    }

    @OnError("Lookup failed for id {id}")
    public String lookup(String id) {
        if (id == null) {
            throw new IllegalArgumentException("null id");
        }
        return "found-" + id;
    }
}
