# NarrativeTrace

> Code is the log.

Zero-code logging that uses your method and parameter names as the log. If the trace is unreadable, your code needs refactoring — not more log statements.

## The problem

Half of this method is logging noise:

```java
public OrderResult placeOrder(String customerId, String productId, int quantity) {
    logger.info("Placing order for customer {} product {} quantity {}", customerId, productId, quantity);

    var inventory = inventoryService.reserve(productId, quantity);
    logger.debug("Reserved inventory: {}", inventory);

    var payment = paymentService.charge(customerId, inventory.total());
    logger.info("Payment processed: {}", payment.transactionId());

    var result = new OrderResult(payment.transactionId(), inventory.items());
    logger.info("Order placed successfully: {}", result);
    return result;
}
```

The business logic is three lines. The logging is another four. Every developer writes these logs differently — different messages, different levels, different included values. The result is inconsistent, verbose, and tangled with the code it describes.

NarrativeTrace eliminates this entirely:

```java
public OrderResult placeOrder(String customerId, String productId, int quantity) {
    var inventory = inventoryService.reserve(productId, quantity);
    var payment = paymentService.charge(customerId, inventory.total());
    return new OrderResult(payment.transactionId(), inventory.items());
}
```

Pure business logic. The trace is generated automatically from the method names, parameter names, and return values — the information that was already there.

## What you get instead

Run your code and get execution traces like this:

```
OrderService.placeOrder(customerId: "C-1234", productId: "SKU-MECHANICAL-KB", quantity: 2)
  CustomerService.findCustomer(customerId: "C-1234") -> Customer[id=C-1234, name=Alice Johnson, tier=GOLD]
  ProductCatalogService.lookupPrice(productId: "SKU-MECHANICAL-KB") -> 89.99
  InventoryService.reserve(productId: "SKU-MECHANICAL-KB", quantity: 2) -> Reservation[productId=SKU-MECHANICAL-KB, quantity=2]
  PaymentService.charge(customerId: "C-1234", amount: 179.98) -> PaymentConfirmation[transactionId=TXN-00001, amount=179.98]
-> OrderResult[orderId=ORD-00001, transactionId=TXN-00001, totalCharged=179.98, itemCount=2]
```

## When something goes wrong

The trace makes bugs visible:

```
OrderService.placeOrder(customerId: "C-BROKE", productId: "SKU-MOUSE-PAD", quantity: 3)
  CustomerService.findCustomer(customerId: "C-BROKE") -> Customer[id=C-BROKE, name=Charlie Broke, tier=STANDARD]
  ProductCatalogService.lookupPrice(productId: "SKU-MOUSE-PAD") -> 24.99
  InventoryService.reserve(productId: "SKU-MOUSE-PAD", quantity: 3) -> Reservation[productId=SKU-MOUSE-PAD, quantity=3]
  PaymentService.charge(customerId: "C-BROKE", amount: 74.97) !! PaymentDeclinedException: Payment declined for customer C-BROKE
!! PaymentDeclinedException: Payment declined for customer C-BROKE
```

`InventoryService.reserve` was called but `InventoryService.release` is nowhere in the trace. The bug is visible.

## The trace is only as good as your names

The same Minecraft "player joins world" flow, traced twice — once with domain names, once with generic names:

**Refactored (clean names):**
```
WorldServer.playerJoined(playerName: "Steve")
  WorldGenerator.generateChunk(x: 0, z: 0) -> Chunk(x: 0, z: 0, biome: "plains")
  PlayerInventory.addItem(item: OAK_LOG, quantity: 4) -> true
  CraftingTable.craft(recipe: WOODEN_PICKAXE) -> WOODEN_PICKAXE
  CreatureSpawner.spawnHostile(type: ZOMBIE, x: 10, y: 64, z: 20) -> Creature(type: ZOMBIE, ...)
```

**Unrefactored (generic names):**
```
GameManager.handle(input: "Steve")
  DataProcessor.process(a: 0, b: 0) -> DataResult(a: 0, b: 0, tag: "plains")
  StateManager.update(type: 1, count: 4) -> true
  ThingFactory.create(type: 1) -> 1
  EntityHandler.execute(kind: 1, a: 10, b: 64, c: 20) -> Entity(kind: 1, ...)
```

Same call graph. Same return values. Only names differ. If your code can't tell its own story, it needs refactoring.

## Why this matters for AI-assisted development

Every `logger.info(...)` line is a line that AI coding tools — Claude Code, Copilot, Cursor — have to parse, spend tokens on, and reason around. In a typical service class, logging is 30–50% of the lines. Remove those lines and you get:

- **More business logic per context window** — the same token budget covers more of your actual code.
- **Cleaner reasoning** — AI sees what the code does, not how it logs what it does.
- **Signal-only diffs** — pull requests show business logic changes, not mixed logic-and-logging changes.

That's not a vague benefit — it's measurable in tokens.

## Clarity scoring

If the trace *is* the code, then trace quality *is* code quality. NarrativeTrace includes a clarity analyzer that scores your method, class, and parameter names:

```
## Clarity Report — Order Placement
Overall: 0.92 (high)

| Element     | Score | Note                    |
|-------------|-------|-------------------------|
| placeOrder  | 1.00  | Strong verb + object    |
| customerId  | 1.00  | Domain-specific noun    |
| processData | 0.30  | Generic verb + generic noun |
```

Generic names like `processData`, `handleRequest`, `result` score low. Domain-specific names like `reserveInventory`, `customerId` score high. The clarity report is generated automatically when running tests with output enabled.

Clarity scoring is still experimental.

## How it compares

**Structured logging** (SLF4J + MDC) requires you to write log statements. NarrativeTrace generates them from your code structure — no `logger.info()` calls, no message formatting, no MDC wiring.

**Distributed tracing** (OpenTelemetry, Jaeger) tracks request flow across services with spans and trace IDs. NarrativeTrace captures method-level call trees with full parameter values and return values — detail that span-based tracers don't record. NarrativeTrace propagates across threads via Micrometer context-propagation and integrates with SLF4J/MDC, so it can complement your existing distributed tracing stack.

**AOP logging** (Spring AOP, AspectJ) auto-logs method entry/exit but produces flat, mechanical output. NarrativeTrace produces nested call trees and scores your naming quality on top.

NarrativeTrace doesn't replace your production alerting or your service topology maps. It gives you something neither provides: a human-readable execution narrative that doubles as a code quality diagnostic.

## Quick start

```bash
java -javaagent:narrativetrace-agent.jar=packages=com.example.myapp.* -jar your-app.jar
```

Instruments all classes in the specified packages using bytecode transformation. No proxy wiring needed. Also available as a [JDK proxy, Spring auto-wrapper, and JUnit 5/4 extension](documentation/installation-guide.md).

## Installation

Java 17+. Add the modules you need:

```kotlin
// build.gradle.kts
tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}

dependencies {
    implementation("ai.narrativetrace:narrativetrace-core:0.1.0")
    implementation("ai.narrativetrace:narrativetrace-proxy:0.1.0")
}
```

The `-parameters` compiler flag is required to preserve method parameter names in bytecode — without it, traces show `arg0`, `arg1` instead of real names.

A Gradle plugin that handles dependencies, compiler flags, and test configuration automatically is in development.

See the [Installation Guide](documentation/installation-guide.md) for agent, Spring, JUnit 5/4, and other integration paths.

## Modules

| Module | You need it when... |
|--------|---------------------|
| `narrativetrace-core` | Always required. Zero runtime dependencies. |
| `narrativetrace-proxy` | Using JDK proxy tracing (most common). |
| `narrativetrace-junit5` | Auto-tracing in JUnit 5 tests. |
| `narrativetrace-junit4` | Auto-tracing in JUnit 4 tests. |
| `narrativetrace-spring` | Auto-wrapping Spring beans. |
| `narrativetrace-slf4j` | Routing traces through SLF4J/Logback. |
| `narrativetrace-clarity` | Analyzing method/param naming quality. |
| `narrativetrace-diagrams` | Generating Mermaid/PlantUML sequence diagrams. |
| `narrativetrace-micrometer` | Cross-thread trace propagation via Micrometer context-propagation. |
| `narrativetrace-agent` | Bytecode-level tracing without proxy wiring. |
| `narrativetrace-servlet` | Production request lifecycle in any servlet app (no Spring required). |
| `narrativetrace-spring-web` | Spring `@Configuration` for `narrativetrace-servlet` — auto-wires filter with `ObjectProvider`. |
| `narrativetrace-examples` | Reference apps (source only): e-commerce (6 scenarios), Minecraft naming comparison, and hotel booking clarity demo. |

**Typical starting point:** `core` + `proxy` + `junit5`.

### Pro tier

Coming soon.

## Documentation

- [Installation Guide](documentation/installation-guide.md) — dependencies, integration paths, trace output setup
- [Configuration Guide](documentation/configuration-guide.md) — tracing levels, JUnit/Gradle/Spring/SLF4J config
- [Annotations Guide](documentation/annotations-guide.md) — `@Narrated`, `@OnError`, `@NotTraced`, `@NarrativeSummary`, `@EnableNarrativeTrace`
- [Clarity Guide](documentation/clarity-guide.md) — scoring model, NLP components, JUnit integration

## Building from source

```bash
./gradlew test                                    # run all tests
./gradlew check                                   # tests + PMD + JaCoCo coverage
./gradlew :narrativetrace-examples:run            # run the e-commerce demo
./gradlew :narrativetrace-examples:runMinecraft   # run the naming comparison demo
./gradlew :narrativetrace-examples:runClarity     # run the clarity scoring demo
./gradlew :narrativetrace-examples:traceTests     # run tests → Markdown trace files
```

## FAQ

### What's the performance overhead?

We've put real effort into making the hot paths efficient, but we won't claim "zero overhead" — tracing does work and work costs something. Here's what we've measured (JMH, JDK 17):

- **Tracing OFF or inactive context:** The proxy adds ~10–16 ns per call over a direct invocation (~21 ns). The bytecode agent adds nothing measurable. An `isActive()` fast-path gate skips all capture, rendering, and reflection work when tracing is disabled.
- **Active tracing:** A traced proxy call with parameter capture and value rendering takes ~200–300 ns depending on annotations. That's the cost of recording one method call — still well under a microsecond.
- **Allocations:** Inactive paths allocate only the JDK proxy's `Object[]` args array (24 B over a direct call). The bytecode agent allocates nothing beyond the direct call. Active paths allocate 680–1008 B/op for the trace node, parameter captures, and rendered strings.

These numbers come from [JMH benchmarks](narrativetrace-benchmarks/) run with `-prof gc`. Baselines are stored in [`baseline.txt`](narrativetrace-benchmarks/baseline.txt) and [`allocation-baseline.txt`](narrativetrace-benchmarks/allocation-baseline.txt) so regressions are visible across commits. Performance is an ongoing concern, not a solved problem.

### How does value serialization work?

NarrativeTrace uses **eager serialization** — parameter values and return values are rendered to strings at the moment of capture, before they're stored in the trace. This is a deliberate design choice:

- **Correctness:** Objects are captured as they were at call time. If a mutable object is modified after the traced call returns, the trace still shows the original value.
- **No object retention:** The trace holds only strings, not references to your domain objects. Nothing prevents your objects from being garbage collected.
- **Safe rendering:** The built-in `ValueRenderer` handles nulls, strings, numbers, enums, records, collections, arrays, and plain objects. It detects cycles (via identity checks), catches rogue `toString()` implementations, and truncates large values. POJOs without a custom `toString()` are rendered by reflecting over their fields.

The trade-off is that serialization happens on every traced call, whether or not you ever look at the trace. The cost is included in the ~200–300 ns active-path numbers above. For most applications this is negligible, but if you're tracing extremely hot loops, use `TracingLevel.OFF` or `@NotTraced` to exclude them.

### Does it trace private methods?

No — the proxy and agent trace interface methods. But private methods are visible through the service calls they make:

```java
private void fulfillOrder(Order order) {
    if (order.isDigital()) {
        deliveryService.sendDownloadLink(order.customerId(), order.productId());
    } else {
        warehouseService.shipPhysical(order.customerId(), order.shippingAddress());
    }
    notificationService.confirmOrder(order.customerId(), order.orderId());
}
```

The trace shows which branch ran:

```
OrderService.placeOrder(customerId: "C-1234", productId: "SKU-EBOOK")
  DeliveryService.sendDownloadLink(customerId: "C-1234", productId: "SKU-EBOOK") -> "https://..."
  NotificationService.confirmOrder(customerId: "C-1234", orderId: "ORD-001") -> true
```

No need to trace the `if` — the presence of `sendDownloadLink` and absence of `shipPhysical` tells the story. For the rare case where you need visibility into logic that doesn't call other services, NarrativeTrace integrates with SLF4J — add a targeted `logger.debug()` and it flows into the same output.

## License

Apache License 2.0. See [LICENSE](LICENSE).
