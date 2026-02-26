# NarrativeTrace Java Configuration Guide

This guide documents runtime and test configuration for NarrativeTrace Java.

## Configuration Surface

NarrativeTrace provides five configuration paths:

| Path | Mechanism | Best for |
|---|---|---|
| Gradle Plugin | `narrativeTrace { }` DSL in `build.gradle.kts` | Gradle projects (recommended) |
| JUnit 5 | `junit-platform.properties` | Test-time trace output |
| Pure Java / Agent | `narrativetrace.properties` on classpath | Standalone apps, agent |
| Gradle (manual) | `gradle.properties` + build script forwarding | Gradle projects without plugin |
| Spring | `@EnableNarrativeTrace` annotation | Spring apps |

All paths support system property overrides (`-D` flags) as the highest-priority source.

## Gradle Plugin DSL

The Gradle plugin (`ai.narrativetrace`) configures everything automatically. Apply it and optionally customize:

```kotlin
plugins {
    id("ai.narrativetrace") version "0.1.0-SNAPSHOT"
}

narrativeTrace {
    enabled.set(true)                  // default: true
    manageDependencies.set(true)       // default: true — adds core, proxy, clarity, diagrams, junit5
    outputDir.set(layout.buildDirectory.dir("narrativetrace"))  // default
    testFramework.set("junit5")        // "junit5" (default) or "junit4"

    clarity {
        minScore.set(0.80)             // default: 0.0 (no gate)
        maxHighIssues.set(0)           // default: Integer.MAX_VALUE (no gate)
        warnOnly.set(false)            // default: false — true logs warnings instead of failing
    }
}
```

### What the plugin does

| Action | Detail |
|---|---|
| Adds `-parameters` compiler flag | On all `JavaCompile` tasks, skipped if already present |
| Adds test dependencies | `core`, `proxy`, `clarity`, `diagrams`, and `junit5` (or `junit4`) on `testImplementation` |
| Sets test JVM properties | `narrativetrace.output=true` and `narrativetrace.outputDir` on all `Test` tasks |
| Registers `clarityCheck` task | Reads `clarity-results.json`, enforces thresholds, wired into `check` lifecycle |

### Disabling features

To manage dependencies yourself (e.g., to pin specific versions):

```kotlin
narrativeTrace {
    manageDependencies.set(false)
}
```

To disable the plugin entirely (e.g., in a subproject):

```kotlin
narrativeTrace {
    enabled.set(false)
}
```

### Clarity thresholds

The `clarityCheck` task reads `build/narrativetrace/clarity-results.json` (produced by the JUnit extension during `test`) and enforces configured thresholds. It runs automatically as part of `./gradlew check`.

- **`minScore`** — minimum overall clarity score (0.0–1.0). Any scenario below this threshold fails the build.
- **`maxHighIssues`** — maximum number of HIGH-severity issues per scenario. Exceeding this fails the build.
- **`warnOnly`** — when `true`, threshold violations produce warnings instead of build failures.

If no `clarity-results.json` exists (e.g., no tests ran), the task passes silently.

## 1. Tracing Levels (`NarrativeTraceConfig`)

`ThreadLocalNarrativeContext` uses `NarrativeTraceConfig`, which defaults to `DETAIL`.

```java
var config = new NarrativeTraceConfig(TracingLevel.NARRATIVE);
var context = new ThreadLocalNarrativeContext(config);
```

Available levels:

| Level | Behavior |
|---|---|
| `OFF` | No tracing captured |
| `ERRORS` | Only exception paths captured |
| `SUMMARY` | Captures root entry, deepest leaf, and full exception chains |
| `NARRATIVE` | Captures full call flow, suppresses parameter values |
| `DETAIL` | Captures full call flow with parameter values and return values |

Runtime level changes are supported:

```java
config.setLevel(TracingLevel.ERRORS);
```

## 2. JUnit 5 Configuration (`junit-platform.properties`)

The JUnit extension uses `ExtensionContext.getConfigurationParameter()`, which resolves values in this order:

1. System properties (highest — CLI `-D` flags still work)
2. `junit-platform.properties` on the test classpath
3. Hardcoded defaults (lowest)

### Properties

| Property | Values | Default |
|---|---|---|
| `narrativetrace.output` | `true` / `false` | `false` |
| `narrativetrace.outputDir` | Any writable path | `build/narrativetrace` |
| `narrativetrace.format` | `markdown`, `text`, `mermaid`, `plantuml` | `markdown` |

### File-based configuration (recommended)

Drop a file in `src/test/resources/junit-platform.properties`:

```properties
narrativetrace.output=true
narrativetrace.format=markdown
```

No Gradle `systemProperty()` wiring needed. The file is test-only and never touches production.

### CLI overrides

System properties still work as overrides:

```bash
./gradlew test -Dnarrativetrace.output=true
./gradlew test -Dnarrativetrace.format=text
./gradlew test -Dnarrativetrace.outputDir=out/narrative
```

### Gradle CLI forwarding (optional)

Only needed if you want to pass CLI `-D` flags through Gradle to the forked test JVM:

```kotlin
tasks.withType<Test> {
    System.getProperty("narrativetrace.output")?.let { systemProperty("narrativetrace.output", it) }
    System.getProperty("narrativetrace.outputDir")?.let { systemProperty("narrativetrace.outputDir", it) }
    System.getProperty("narrativetrace.format")?.let { systemProperty("narrativetrace.format", it) }
}
```

### Scenario Names

The extension derives a human-readable scenario name from each test:

- `customerPlacesOrder()` → "Customer places order"
- `customer_places_order()` → "Customer places order"
- `@DisplayName("customer places order")` → "customer places order" (passed through)

JUnit parameter type suffixes (e.g. `(NarrativeContext)`) are stripped automatically.

### File Layout

Base layout:

- `<outputDir>/traces/<TestClassSimpleName>/<test_method_slug>.<ext>`

When `format=markdown`, the extension also writes per test:

- Mermaid diagram: `<outputDir>/diagrams/<TestClassSimpleName>/<test_method_slug>.mmd`
- JSON export: `<outputDir>/traces/<TestClassSimpleName>/<test_method_slug>.json`

After all tests in a class complete, the extension writes:

- Clarity report: `<outputDir>/clarity-report.md`
- Console summary (printed to stdout):
  ```
  NarrativeTrace — Suite complete
    2 scenarios recorded
    Clarity: 100% high | 0% moderate | 0% low
    Reports: build/narrativetrace
  ```

## 3. Pure Java / Agent Configuration (`narrativetrace.properties`)

For standalone Java apps and the bytecode agent, `ConfigResolver` loads configuration from the classpath.

Resolution order:

1. System properties (highest)
2. `narrativetrace.properties` on classpath
3. Hardcoded defaults (lowest)

### Properties

| Property | Values | Default |
|---|---|---|
| `narrativetrace.level` | `OFF`, `ERRORS`, `SUMMARY`, `NARRATIVE`, `DETAIL` | `DETAIL` |
| `narrativetrace.packages` | Semicolon-separated package prefixes | (empty) |

### File-based configuration

Drop a file on the classpath (e.g. `src/main/resources/narrativetrace.properties`):

```properties
narrativetrace.level=DETAIL
narrativetrace.packages=com.example.app.*;com.example.shared.*
```

### Programmatic usage

```java
var resolver = new ConfigResolver();
var level = resolver.resolve("narrativetrace.level", "DETAIL");
```

### Duplicate file detection

If multiple `narrativetrace.properties` files are found on the classpath (e.g., one in the app JAR and one in a dependency), `ConfigResolver` throws `DuplicateConfigurationException` listing all locations. This prevents silent shadowing bugs.

### Agent fallback

When the agent receives no CLI arguments, it falls back to `ConfigResolver`:

```bash
# Explicit CLI args (highest priority)
java -javaagent:narrativetrace-agent.jar=packages=com.example.app -jar app.jar

# Falls back to narrativetrace.properties on classpath
java -javaagent:narrativetrace-agent.jar -jar app.jar
```

## 4. Gradle Configuration (`gradle.properties`)

For Gradle projects, `gradle.properties` provides a single place to define NarrativeTrace test output settings. Properties defined here are available as Gradle project properties and can be forwarded to the forked test JVM.

### Define properties

Add to `gradle.properties` in the project root:

```properties
narrativetrace.output=true
narrativetrace.format=markdown
```

### Forward to test JVM

Gradle project properties don't automatically flow into forked test JVMs. Add forwarding in `build.gradle.kts`:

```kotlin
tasks.withType<Test> {
    useJUnitPlatform()
    listOf("narrativetrace.output", "narrativetrace.outputDir", "narrativetrace.format")
        .forEach { key ->
            (findProperty(key) as? String)?.let { systemProperty(key, it) }
        }
}
```

This reads each property from `gradle.properties` (or CLI `-P` flags) and passes it as a system property to the test JVM. System properties take the highest priority in JUnit's `getConfigurationParameter()` resolution.

### CLI overrides with `-P`

Gradle project properties can be overridden from the command line with `-P`:

```bash
./gradlew test -Pnarrativetrace.format=text
./gradlew test -Pnarrativetrace.output=false
```

### JUnit-specific DSL

Gradle also offers a JUnit-specific way to pass configuration parameters directly:

```kotlin
tasks.withType<Test> {
    useJUnitPlatform {
        configurationParameter("narrativetrace.output", "true")
        configurationParameter("narrativetrace.format", "markdown")
    }
}
```

This only feeds into JUnit's `getConfigurationParameter()` — it does not affect `ConfigResolver` or the agent. Use `gradle.properties` with forwarding when you need a single config source for all integrations.

## 5. Spring Configuration

Use package filters to control which beans are considered for proxy wrapping:

```java
@Configuration
@EnableNarrativeTrace
public class AppConfig { }
```

When `basePackages` is omitted, it defaults to the annotated class's package — just like `@ComponentScan`. To narrow scope explicitly:

```java
@EnableNarrativeTrace(basePackages = {"com.example.order", "com.example.payment"})
```

Spring apps use their own configuration conventions. The `@EnableNarrativeTrace` annotation is the recommended approach — no properties files needed.

Behavior:

- Beans outside `basePackages` (or the default package) are skipped.
- Beans with no interfaces are skipped (JDK dynamic proxy limitation).
- Only interfaces in the configured packages are traced; Spring framework interfaces are ignored.
- A `NarrativeContext` bean is provided automatically.

## 6. SLF4J Configuration

Route trace events through your existing logging framework using `Slf4jNarrativeContext`:

```java
var delegate = new ThreadLocalNarrativeContext();
var context = new Slf4jNarrativeContext(delegate);
```

### Log levels

Events are logged under the `narrativetrace` logger at these default levels:

| Event type | Default level |
|---|---|
| Method entry | `TRACE` |
| Method return | `TRACE` |
| Method exception | `WARN` |

### Custom log levels

Override the defaults by passing a level map:

```java
var context = new Slf4jNarrativeContext(delegate, Map.of(
    Slf4jNarrativeContext.EventType.ENTRY, Level.DEBUG,
    Slf4jNarrativeContext.EventType.RETURN, Level.DEBUG,
    Slf4jNarrativeContext.EventType.EXCEPTION, Level.ERROR
));
```

### MDC fields

`Slf4jNarrativeContext` sets MDC fields on each trace event:

| MDC key | Value |
|---|---|
| `nt.class` | Simple class name of the traced service |
| `nt.method` | Method name |
| `nt.depth` | Call depth (0 for top-level) |

Use these in logback patterns for structured log output.

### Coexisting with traditional logging

Well-structured code — small methods with clear names, computed values returned rather than logged — needs no SLF4J calls at all. NarrativeTrace captures everything from the method signatures and return values.

In code that isn't fully structured that way yet, you can mix in traditional SLF4J calls for things like intermediate computations or decision points that don't surface in method boundaries. The two interleave naturally:

```java
public class DefaultOrderService implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(DefaultOrderService.class);

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
```

```
TRACE [narrativetrace]        → OrderService.placeOrder(customerId: C-1234, productId: SKU-MECHANICAL-KB, quantity: 2)
INFO  [DefaultOrderService]   Placing order: customer=C-1234, product=SKU-MECHANICAL-KB, qty=2
TRACE [narrativetrace]        → CustomerService.findCustomer(customerId: C-1234)
TRACE [narrativetrace]        ← returned: Customer[id=C-1234, name=Alice Johnson, tier=GOLD]
DEBUG [DefaultOrderService]   Resolved customer Alice Johnson (tier: GOLD)
TRACE [narrativetrace]        → ProductCatalogService.lookupPrice(productId: SKU-MECHANICAL-KB)
TRACE [narrativetrace]        ← returned: 89.99
DEBUG [DefaultOrderService]   Calculated total: 89.99 x 2 = 179.98
...
INFO  [DefaultOrderService]   Payment TXN-00001 confirmed for $179.98
TRACE [narrativetrace]        ← returned: OrderResult[orderId=ORD-00001, ...]
```

This makes NarrativeTrace easy to adopt incrementally — add it alongside existing logging, then remove the manual log calls as you refactor toward cleaner method boundaries.

### Legacy logging frameworks

Apps using `java.util.logging` or Log4j 1.x: add the appropriate SLF4J bridge ([jul-to-slf4j](https://www.slf4j.org/legacy.html#jul-to-slf4j) or [log4j-over-slf4j](https://www.slf4j.org/legacy.html#log4j-over-slf4j)) and NarrativeTrace output flows into your existing logging infrastructure unchanged.

## 7. Recommended Defaults by Environment

| Environment | Suggested level | Suggested output |
|---|---|---|
| Local feature work | `DETAIL` | `narrativetrace.output=true`, `format=markdown` |
| CI test runs | `NARRATIVE` or `SUMMARY` | `output=true`, `format=markdown` |
| Performance-sensitive prod | `ERRORS` (or `OFF`) | no test file output |

## See also

- [Installation Guide](installation-guide.md) — dependencies, integration paths, Java agent setup
- [Annotations Guide](annotations-guide.md) — `@Narrated`, `@OnError`, `@NotTraced`, `@NarrativeSummary`, `@EnableNarrativeTrace`
- [Clarity Guide](clarity-guide.md) — scoring model, NLP components, JUnit integration
