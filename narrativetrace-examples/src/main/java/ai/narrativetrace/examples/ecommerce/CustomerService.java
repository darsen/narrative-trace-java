package ai.narrativetrace.examples.ecommerce;

import ai.narrativetrace.core.annotation.OnError;

public interface CustomerService {
    @OnError(value = "Customer {customerId} not found", exception = IllegalArgumentException.class)
    Customer findCustomer(String customerId);
}
