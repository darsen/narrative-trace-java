package ai.narrativetrace.examples.ecommerce;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class DefaultOrderService implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(DefaultOrderService.class);

    private final CustomerService customers;
    private final ProductCatalogService catalog;
    private final InventoryService inventory;
    private final PaymentService payments;
    private final AtomicInteger orderCounter = new AtomicInteger(1);

    public DefaultOrderService(CustomerService customers,
                               ProductCatalogService catalog,
                               InventoryService inventory,
                               PaymentService payments) {
        this.customers = customers;
        this.catalog = catalog;
        this.inventory = inventory;
        this.payments = payments;
    }

    @Override
    public OrderResult placeOrder(String customerId, String productId, int quantity) {
        log.info("Placing order: customer={}, product={}, qty={}", customerId, productId, quantity);

        var customer = customers.findCustomer(customerId);
        log.debug("Resolved customer {} (tier: {})", customer.name(), customer.tier());

        double unitPrice = catalog.lookupPrice(productId);
        double total = unitPrice * quantity;
        log.debug("Calculated total: {} x {} = {}", unitPrice, quantity, total);

        inventory.reserve(productId, quantity);
        var payment = payments.charge(customerId, total, "tok_" + customer.id());
        log.info("Payment {} confirmed for ${}", payment.transactionId(), payment.amount());

        var orderId = "ORD-%05d".formatted(orderCounter.getAndIncrement());
        return new OrderResult(orderId, payment.transactionId(), total, quantity);
    }
}
