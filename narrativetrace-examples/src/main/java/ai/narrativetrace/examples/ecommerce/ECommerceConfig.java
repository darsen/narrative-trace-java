package ai.narrativetrace.examples.ecommerce;

import ai.narrativetrace.core.context.NarrativeContext;
import ai.narrativetrace.core.context.ThreadLocalNarrativeContext;
import ai.narrativetrace.micrometer.NarrativeTraceThreadLocalAccessor;
import ai.narrativetrace.slf4j.Slf4jNarrativeContext;
import ai.narrativetrace.spring.EnableNarrativeTrace;
import io.micrometer.context.ContextRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableNarrativeTrace
@EnableAsync
public class ECommerceConfig {

    @Bean
    NarrativeContext narrativeContext() {
        var tlc = new ThreadLocalNarrativeContext();
        var accessor = new NarrativeTraceThreadLocalAccessor(tlc);
        ContextRegistry.getInstance().registerThreadLocalAccessor(accessor);
        return new Slf4jNarrativeContext(tlc);
    }

    @Bean
    ThreadPoolTaskExecutor taskExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setTaskDecorator(new ContextPropagatingTaskDecorator());
        executor.setCorePoolSize(2);
        executor.setThreadNamePrefix("async-notify-");
        executor.initialize();
        return executor;
    }

    @Bean
    CustomerService customerService() {
        return new InMemoryCustomerService();
    }

    @Bean
    ProductCatalogService catalogService() {
        return new InMemoryProductCatalogService();
    }

    @Bean
    InventoryService inventoryService() {
        return new InMemoryInventoryService();
    }

    @Bean
    PaymentService paymentService() {
        return new InMemoryPaymentService();
    }

    @Bean
    NotificationService notificationService() {
        return new JsonPlaceholderNotificationService();
    }

    @Bean
    OrderService orderService(CustomerService customerService, ProductCatalogService catalogService,
                              InventoryService inventoryService, PaymentService paymentService) {
        return new DefaultOrderService(customerService, catalogService,
                inventoryService, paymentService);
    }
}
