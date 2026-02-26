# NarrativeTrace Java Annotations Guide

This guide lists all annotations available in the NarrativeTrace Java project and explains when and how to use each one.

NarrativeTrace follows a **Code is the Log** philosophy: method names, parameter names, and return values should already communicate the runtime story. Keep business logic clean and expressive first, then use annotations exceptionally, not by default. Add annotations only when they provide concrete additional value, such as targeted narration, error-specific context, or sensitive-data redaction.

## Annotation Inventory

| Annotation | Module | Target | Purpose |
|---|---|---|---|
| `@Narrated` | `narrativetrace-core` | Method | Adds human-readable narration text to a traced method. |
| `@OnError` | `narrativetrace-core` | Method | Adds contextual error text when a method throws. |
| `@NotTraced` | `narrativetrace-core` | Parameter | Marks a parameter value as redacted in trace output. |
| `@NarrativeSummary` | `narrativetrace-core` | Method | Provides custom value rendering for objects in traces. |
| `@EnableNarrativeTrace` | `narrativetrace-spring` | Type (`@Configuration`) | Enables Spring auto-proxy tracing for selected packages. |

## Core Annotations

### `@Narrated`

Use `@Narrated` on methods when you want an explicit sentence in the trace instead of relying only on method name + parameters.

```java
public interface OrderService {
    @Narrated("Placing order of {quantity} units for customer {customerId}")
    OrderResult placeOrder(String customerId, int quantity);
}
```

How it works:

- Template placeholders use parameter names (for example `{customerId}`).
- Works with proxy tracing and agent-based tracing.
- Enriches trace output at all levels with human-readable narration text.

### `@OnError`

Use `@OnError` to attach context-specific messages for exceptions.

```java
public interface PaymentService {
    @OnError(value = "Payment declined for {customerId}, amount was {amount}",
             exception = PaymentDeclinedException.class)
    @OnError(value = "Temporary payment failure for {customerId}",
             exception = ExternalServiceException.class)
    PaymentConfirmation charge(String customerId, double amount, @NotTraced String token);
}
```

How it works:

- You can declare multiple `@OnError` annotations on the same method (it is repeatable).
- If multiple match, the most specific exception type is chosen.
- A bare `@OnError("...")` is equivalent to `exception = Throwable.class`.

The `@OnErrors` container annotation exists behind repeatable `@OnError`. In normal code, you never write it directly — just stack multiple `@OnError` annotations.

### `@NotTraced`

Use `@NotTraced` on sensitive parameters so values are redacted.

```java
public interface AuthService {
    Session login(String username, @NotTraced String password);
}
```

How it works:

- Parameter is flagged as redacted in captured method signatures.
- Prevents sensitive values from appearing in trace content.
- Typical use cases: passwords, tokens, secrets, card data.

### `@NarrativeSummary`

Use `@NarrativeSummary` on a zero-argument method that returns a short summary string for value rendering.

```java
public record Customer(String id, String name, CustomerTier tier) {
    @NarrativeSummary
    public String toNarrativeSummary() {
        return "Customer[id=%s, tier=%s]".formatted(id, tier);
    }
}
```

How it works:

- `ValueRenderer` looks for a public method annotated with `@NarrativeSummary` and no parameters.
- If found, that method output is used in traces.
- If not found, rendering falls back to record/toString behavior.

## Spring Annotations

### `@EnableNarrativeTrace`

Use `@EnableNarrativeTrace` on a Spring configuration class to auto-wrap beans with NarrativeTrace proxies.

```java
@Configuration
@EnableNarrativeTrace(basePackages = {"com.example.orders", "com.example.payments"})
public class AppConfig {
}
```

How it works:

- Registers NarrativeTrace Spring configuration and bean post-processor.
- Only beans in `basePackages` are considered.
- Beans must implement interfaces to be proxied (JDK dynamic proxies).

## Complete Example

```java
public interface TransferService {
    @Narrated("Transferring {amount} from {fromAccountId} to {toAccountId}")
    @OnError(value = "Transfer rejected for source account {fromAccountId}",
             exception = IllegalStateException.class)
    TransferResult transfer(
            String fromAccountId,
            String toAccountId,
            double amount,
            @NotTraced String authToken
    );
}
```

This single method combines narration, targeted error context, and parameter redaction.

## Template Validation

`@Narrated` and `@OnError` templates use `{paramName}` and `{param.property}` placeholders. If a placeholder doesn't match any parameter — for example `{custmerId}` instead of `{customerId}` — the literal `{custmerId}` survives in the resolved trace output.

NarrativeTrace detects this automatically during tests. After each test, the JUnit 5 extension and JUnit 4 rule scan every trace node for unresolved `{...}` patterns and print a warning:

```
WARNING: Unresolved template placeholder(s) detected:
  - OrderService.placeOrder: {custmerId} in narration
```

No configuration is needed — warnings appear in console output whenever the extension or rule is active. This catches typos in placeholder names and invalid property paths (for example `{order.stauts}`) as soon as a test exercises the annotated method.

## See also

- [Installation Guide](installation-guide.md) — dependencies, integration paths, trace output setup
- [Configuration Guide](configuration-guide.md) — tracing levels, JUnit/Gradle/Spring/SLF4J configuration
- [Clarity Guide](clarity-guide.md) — scoring model, NLP components, JUnit integration
