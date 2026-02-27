# NarrativeTrace Java — Complete Reference

> Code is the log. Method names, parameter names, and return values already describe what code does. NarrativeTrace generates human-readable execution traces automatically — no log statements needed.

## Overview

NarrativeTrace is a Java library that auto-generates execution traces from method/parameter names and return values. It wraps services with tracing proxies (or uses a Java agent) to capture every method call, then renders the captured trace as Markdown, prose, Mermaid diagrams, PlantUML, or JSON.

The core insight: if your method is called `placeOrder(customerId, quantity)` and returns an `OrderResult`, you don't need `logger.info("Placing order...")`. The method signature already says it. NarrativeTrace captures that information and renders it as a readable execution trace.

**Key features:**
- Zero-config trace capture via JDK proxy or Java agent
- Multiple output formats: Markdown, prose, indented text, Mermaid, PlantUML, JSON
- JUnit 5 and JUnit 4 extensions with automatic per-test output
- Naming clarity analysis that scores code readability (method, class, parameter names)
- Spring integration with automatic bean post-processing
- Servlet filter for production request lifecycle tracing
- SLF4J bridge with MDC integration
- Micrometer context-propagation for cross-thread tracing
- Custom annotations: `@Narrated`, `@OnError`, `@NotTraced`, `@NarrativeSummary`

**Requirements:** Java 17+, `-parameters` compiler flag

**Group ID:** `ai.narrativetrace`

---

## Quick Start

### 1. Add the Gradle plugin (recommended)

```kotlin
plugins {
    id("ai.narrativetrace") version "0.1.0-SNAPSHOT"
}
```

This adds all dependencies, the `-parameters` compiler flag, and test JVM configuration automatically.

### 2. Write a test

```java
@ExtendWith(NarrativeTraceExtension.class)
class OrderServiceTest {
    @Test
    void customerPlacesOrder(NarrativeContext context) {
        OrderService service = NarrativeTraceProxy.trace(
            new DefaultOrderService(), OrderService.class, context);
        service.placeOrder("C-1234", 2);
    }
}
```

### 3. Run tests

```bash
./gradlew test
```

Output appears in `build/narrativetrace/`:
```
build/narrativetrace/
├── traces/
│   └── OrderServiceTest/
│       ├── customer_places_order.md
│       ├── customer_places_order.json
│       └── customer_places_order.mmd
├── diagrams/
│   └── OrderServiceTest/
│       └── customer_places_order.mmd
└── clarity-report.md
```

### 4. Without the plugin (manual setup)

```kotlin
// build.gradle.kts
tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}

dependencies {
    implementation("ai.narrativetrace:narrativetrace-core:0.1.0-SNAPSHOT")
    implementation("ai.narrativetrace:narrativetrace-proxy:0.1.0-SNAPSHOT")
    testImplementation("ai.narrativetrace:narrativetrace-junit5:0.1.0-SNAPSHOT")
}
```

---

## Module Map

NarrativeTrace consists of 14 modules. Most projects need only 2-3.

### Core modules (always needed)

| Module | Artifact | Purpose |
|--------|----------|---------|
| `narrativetrace-core` | `ai.narrativetrace:narrativetrace-core` | Records, sealed interfaces, context, renderers, config, export SPI. Zero runtime dependencies. |
| `narrativetrace-proxy` | `ai.narrativetrace:narrativetrace-proxy` | JDK dynamic proxy for interface-based tracing. Depends on core. |

### Test framework integrations

| Module | Artifact | Purpose |
|--------|----------|---------|
| `narrativetrace-junit5` | `ai.narrativetrace:narrativetrace-junit5` | JUnit 5 extension: auto context, output files, clarity reports, console summaries. |
| `narrativetrace-junit4` | `ai.narrativetrace:narrativetrace-junit4` | JUnit 4 rules: `NarrativeTraceRule` + `NarrativeTraceClassRule`. |

### Output format modules

| Module | Artifact | Purpose |
|--------|----------|---------|
| `narrativetrace-diagrams` | `ai.narrativetrace:narrativetrace-diagrams` | Mermaid and PlantUML sequence diagram renderers. |
| `narrativetrace-clarity` | `ai.narrativetrace:narrativetrace-clarity` | NLP-based naming clarity analysis and scoring. |

### Integration modules

| Module | Artifact | Purpose |
|--------|----------|---------|
| `narrativetrace-spring` | `ai.narrativetrace:narrativetrace-spring` | `@EnableNarrativeTrace`, BeanPostProcessor for auto-wrapping. Requires Spring Context 6.2+. |
| `narrativetrace-servlet` | `ai.narrativetrace:narrativetrace-servlet` | Servlet filter for per-request trace lifecycle. Zero Spring dependencies. |
| `narrativetrace-spring-web` | `ai.narrativetrace:narrativetrace-spring-web` | Spring auto-configuration for the servlet filter with pluggable exporter. |
| `narrativetrace-slf4j` | `ai.narrativetrace:narrativetrace-slf4j` | SLF4J bridge with MDC integration. |
| `narrativetrace-micrometer` | `ai.narrativetrace:narrativetrace-micrometer` | Micrometer `ThreadLocalAccessor` for cross-thread trace propagation. |
| `narrativetrace-agent` | `ai.narrativetrace:narrativetrace-agent` | Java agent for bytecode-level instrumentation via ASM. |

### Build tooling

| Module | Artifact | Purpose |
|--------|----------|---------|
| `narrativetrace-gradle-plugin` | `ai.narrativetrace:narrativetrace-gradle-plugin` | Gradle plugin: auto deps, `-parameters`, `clarityCheck` task, `clarityScan` task. |

### Dependency graph

```
narrativetrace-core (zero deps)
├── narrativetrace-proxy → core
├── narrativetrace-diagrams → core
├── narrativetrace-clarity → core
├── narrativetrace-slf4j → core, slf4j-api
├── narrativetrace-micrometer → core, micrometer context-propagation
├── narrativetrace-agent → core (ASM bundled)
├── narrativetrace-servlet → core, servlet-api (compileOnly)
├── narrativetrace-spring → core, proxy, spring-context
├── narrativetrace-spring-web → servlet, spring, spring-context
├── narrativetrace-junit5 → core, proxy, diagrams, clarity, junit-jupiter
└── narrativetrace-junit4 → core, proxy, diagrams, clarity, junit:junit
```

---

## Core API Reference

### NarrativeContext (interface)

The central interface for recording trace events. All tracing flows through this.

```java
package ai.narrativetrace.core.context;

public interface NarrativeContext {
    boolean isActive();
    void enterMethod(MethodSignature signature);
    void exitMethodWithReturn(String renderedReturnValue);
    void exitMethodWithException(Throwable exception, String errorContext);
    TraceTree captureTrace();
    void reset();
    ContextSnapshot snapshot();
}
```

**Key methods:**
- `enterMethod(signature)` — push a frame. Must have a matching exit call.
- `exitMethodWithReturn(rendered)` — pop frame, record success. The value is pre-rendered to String.
- `exitMethodWithException(ex, errorContext)` — pop frame, record failure.
- `captureTrace()` — return the immutable trace tree.
- `reset()` — clear all state. Call between tests/requests.
- `snapshot()` — create a `ContextSnapshot` for cross-thread propagation.

**Implementations:**
- `ThreadLocalNarrativeContext` — default, zero-dependency, ThreadLocal-based
- `Slf4jNarrativeContext` — decorator that routes events through SLF4J
- `NoopNarrativeContext` — discards everything (for disabled tracing)

### ThreadLocalNarrativeContext

Default implementation using a ThreadLocal Deque-based call stack.

```java
// Default (DETAIL level)
var context = new ThreadLocalNarrativeContext();

// With explicit level
var config = new NarrativeTraceConfig(TracingLevel.NARRATIVE);
var context = new ThreadLocalNarrativeContext(config);

// Change level at runtime (thread-safe, volatile)
config.setLevel(TracingLevel.ERRORS);
```

### TracingLevel (enum)

Controls trace capture verbosity. Ordered by increasing detail:

| Level | Captures | Parameter Values |
|-------|----------|-----------------|
| `OFF` | Nothing | N/A |
| `ERRORS` | Exception paths only | No |
| `SUMMARY` | Root + leaf calls only | No |
| `NARRATIVE` | All calls | No |
| `DETAIL` | All calls | Yes |

Default: `DETAIL` for tests, `ERRORS` or `OFF` recommended for production.

### NarrativeTraceProxy

Creates JDK dynamic proxies that automatically capture trace events.

```java
// Single interface
OrderService traced = NarrativeTraceProxy.trace(realService, OrderService.class, context);

// Multiple interfaces
Object traced = NarrativeTraceProxy.trace(target, new Class<?>[]{ServiceA.class, ServiceB.class}, context);
```

**Requirements:**
- Target must implement at least one interface
- `-parameters` compiler flag for meaningful parameter names
- Works with lambdas and non-public implementations

**Proxy vs Agent:** Use the proxy when you control instantiation and the target implements interfaces. Use the agent for concrete classes or third-party code.

### TraceTree, TraceNode, and data model

```java
// TraceTree — immutable result of trace capture
public interface TraceTree {
    List<TraceNode> roots();
    boolean isEmpty();
}

// TraceNode — single method invocation
public record TraceNode(
    MethodSignature signature,
    List<TraceNode> children,
    TraceOutcome outcome,
    long durationNanos
) {}

// MethodSignature — identifies the method
public record MethodSignature(
    String className,
    String methodName,
    List<ParameterCapture> parameters,
    String narration,        // resolved @Narrated template, or null
    String errorContext       // resolved @OnError template, or null
) {}

// ParameterCapture — captured parameter
public record ParameterCapture(
    String name,             // from -parameters flag
    String renderedValue,    // pre-rendered String
    boolean redacted         // true if @NotTraced
) {}

// TraceOutcome — how the method completed
public sealed interface TraceOutcome {
    record Returned(String renderedValue) implements TraceOutcome {}
    record Threw(Throwable exception) implements TraceOutcome {}
}
```

**Important:** Values are eagerly serialized at capture time. `renderedValue` fields hold pre-rendered Strings. String values include quotes (`"\"order-42\""`), numbers/booleans are plain (`"42"`, `"true"`). Empty string `""` means suppressed (non-DETAIL level).

### ContextSnapshot (cross-thread propagation)

```java
var snapshot = context.snapshot();

// Pass to another thread
executor.submit(snapshot.wrap(() -> {
    // Trace events captured in parent context
    service.processOrder(orderId);
}));

// Or manual activation
try (var scope = snapshot.activate()) {
    service.processOrder(orderId);
}
```

Convenience wrappers: `wrap(Runnable)`, `wrap(Callable)`, `wrap(Supplier)`.

### NarrativeRenderer (interface)

```java
public interface NarrativeRenderer {
    String render(TraceTree tree);
}
```

**Built-in implementations:**
- `MarkdownRenderer` — Markdown with YAML frontmatter
- `ProseRenderer` — natural-language sentences
- `IndentedTextRenderer` — plain text with arrow notation

### ValueRenderer

Serializes objects to strings. Handles:
- Primitives, strings, enums
- Arrays and collections
- Records (component-based)
- POJOs via reflective getter introspection
- `@NarrativeSummary`-annotated methods
- Cycle detection (identity-based)
- toString failure protection

---

## Annotations Reference

### @Narrated

Customizes the narrative template for a traced method.

```java
@Narrated("place order for {item} with quantity {quantity}")
OrderResult placeOrder(String item, int quantity);

@Narrated("transfer {amount} from {source.accountId} to {target.accountId}")
TransferResult transfer(Account source, Account target, Money amount);
```

- `{paramName}` — renders the parameter value
- `{param.property}` — calls the getter on the raw object (before serialization)
- Templates are resolved at capture time in the proxy/agent

### @OnError / @OnErrors

Exception-specific narrative templates. Repeatable.

```java
@OnError(value = "order {orderId} rejected: insufficient stock",
         exception = InsufficientStockException.class)
@OnError(value = "order {orderId} failed: payment declined",
         exception = PaymentDeclinedException.class)
OrderResult placeOrder(String orderId, int quantity);
```

- Exception matching uses `isAssignableFrom` — most specific wins
- Default `exception = Throwable.class` matches all
- Same `{paramName}` placeholder syntax as `@Narrated`

### @NotTraced

Redacts a parameter value in all output channels.

```java
void authenticate(String username, @NotTraced String password);
// Output: authenticate username="admin" password=***
```

### @NarrativeSummary

Marks a no-arg method as the custom renderer for its type.

```java
public class Order {
    @NarrativeSummary
    public String narrativeSummary() {
        return "Order " + id + " ($" + total + ")";
    }
}
```

The annotated method must be public, no-arg, and return String.

---

## Configuration Reference

### System properties (highest priority)

| Property | Values | Default |
|----------|--------|---------|
| `narrativetrace.output` | `true`/`false` | `false` |
| `narrativetrace.outputDir` | path | `build/narrativetrace` |
| `narrativetrace.format` | `markdown`, `text`, `mermaid`, `plantuml` | `markdown` |
| `narrativetrace.level` | `OFF`, `ERRORS`, `SUMMARY`, `NARRATIVE`, `DETAIL` | `DETAIL` |
| `narrativetrace.packages` | semicolon-separated prefixes | (empty) |

### JUnit 5: junit-platform.properties

```properties
# src/test/resources/junit-platform.properties
narrativetrace.output=true
narrativetrace.format=markdown
```

### Gradle plugin DSL

```kotlin
plugins {
    id("ai.narrativetrace") version "0.1.0-SNAPSHOT"
}

narrativeTrace {
    enabled.set(true)
    manageDependencies.set(true)
    outputDir.set(layout.buildDirectory.dir("narrativetrace"))
    testFramework.set("junit5")  // or "junit4"

    clarity {
        minScore.set(0.80)
        maxHighIssues.set(0)
        warnOnly.set(false)
    }
}
```

The plugin:
- Adds `-parameters` to all JavaCompile tasks
- Adds test dependencies (core, proxy, clarity, diagrams, junit5/junit4)
- Sets test JVM properties
- Registers `clarityCheck` task (wired into `check`)
- Registers `clarityScan` task (standalone classpath analysis)

### Spring configuration

```java
@Configuration
@EnableNarrativeTrace(basePackages = "com.example.service")
public class AppConfig { }
```

- `basePackages` limits which beans are proxy-wrapped
- Empty = all interface-implementing beans
- BPP runs at `HIGHEST_PRECEDENCE` (inner proxy with `@Async`)

Override the context bean:

```java
@Bean
public static NarrativeContext narrativeContext() {
    return new Slf4jNarrativeContext(new ThreadLocalNarrativeContext());
}
```

### Agent configuration

```bash
java -javaagent:narrativetrace-agent.jar=packages=com.example.app.*;com.example.shared.* -jar app.jar
```

- Package separators: semicolons (`;`)
- Wildcards: `com.example.*` matches all subpackages
- Without CLI args, falls back to `narrativetrace.properties` on classpath

---

## Integration Guides

### JDK Proxy (any Java app)

```java
var context = new ThreadLocalNarrativeContext();
var traced = NarrativeTraceProxy.trace(orderService, OrderService.class, context);

traced.placeOrder("C-1234", 2);

// Render
String output = new IndentedTextRenderer().render(context.captureTrace());
System.out.println(output);
context.reset();
```

### JUnit 5

```java
@ExtendWith(NarrativeTraceExtension.class)
class OrderServiceTest {
    @Test
    void placesOrder(NarrativeContext context) {
        var service = NarrativeTraceProxy.trace(impl, OrderService.class, context);
        service.placeOrder("C-1234", 2);
    }
}
```

Features:
- Per-test `NarrativeContext` via parameter injection
- Automatic failure trace printing
- Scenario names derived from test method names
- With `narrativetrace.output=true`: writes `.md`, `.json`, `.mmd` per test
- Suite-level clarity report and console summary after all tests

### JUnit 4

```java
public class OrderServiceTest {
    @ClassRule public static NarrativeTraceClassRule classRule = new NarrativeTraceClassRule();
    @Rule public NarrativeTraceRule rule = classRule.testRule();

    @Test
    public void placesOrder() {
        var service = NarrativeTraceProxy.trace(impl, OrderService.class, rule.context());
        service.placeOrder("C-1234", 2);
    }
}
```

- `rule.context()` provides the per-test context
- Configuration via system properties (`-Dnarrativetrace.output=true`)
- `NarrativeTraceClassRule` accumulates traces for combined clarity reports

### Spring

```java
@Configuration
@EnableNarrativeTrace(basePackages = "com.example")
@EnableAsync
public class AppConfig {
    @Bean
    public static NarrativeContext narrativeContext() {
        return new Slf4jNarrativeContext(new ThreadLocalNarrativeContext());
    }
}
```

Spring beans implementing interfaces within `basePackages` are automatically wrapped. The BPP ordering ensures tracing runs on the async thread when combined with `@EnableAsync`.

### Servlet filter (production)

```java
// Zero Spring dependencies
var context = new ThreadLocalNarrativeContext();
var filter = new NarrativeTraceFilter(context, new Slf4jTraceExporter());
// Register filter with your servlet container
```

Filter lifecycle: reset → chain → capture → export → reset.

`Slf4jTraceExporter` logs to `narrativetrace.export` at INFO:
```
GET /api/orders [200] 42ms — {"nodes":[...]}
```

### Spring Web (auto-configured servlet filter)

```java
@Configuration
@EnableNarrativeTrace(basePackages = "com.example")
@Import(NarrativeTraceWebConfiguration.class)
public class WebConfig { }
```

Registers `NarrativeTraceFilter` as a Spring bean with the context and exporter from the application context.

### SLF4J bridge

```java
var delegate = new ThreadLocalNarrativeContext();
var context = new Slf4jNarrativeContext(delegate, Map.of(
    EventType.ENTRY, Level.DEBUG,
    EventType.RETURN, Level.DEBUG,
    EventType.EXCEPTION, Level.ERROR
));
```

MDC keys set during logging: `nt.class`, `nt.method`, `nt.depth`.

Default levels: ENTRY=TRACE, RETURN=TRACE, EXCEPTION=WARN.

### Micrometer cross-thread propagation

```java
// Register once at startup
ContextRegistry.getInstance().registerThreadLocalAccessor(
    new NarrativeTraceThreadLocalAccessor(context));
```

Enables automatic trace propagation with Spring Boot 3, Reactor, and `@Async`.

### Java agent

```bash
java -javaagent:narrativetrace-agent.jar=packages=com.example.* -jar app.jar
```

- Instruments all public methods in matching packages
- Uses ASM AdviceAdapter with try-catch-rethrow
- No source code changes required

---

## Clarity Scoring

The clarity module scores naming quality across five dimensions:

| Component | Weight | Measures |
|-----------|--------|----------|
| Method names | 30% | Verb quality, domain vocabulary, specificity |
| Parameter names | 25% | Domain specificity vs generic tokens |
| Class names | 20% | Role suffix quality, domain prefix |
| Structural | 15% | Parameter count, call depth |
| Cohesion | 10% | Vocabulary consistency within classes |

### Score interpretation

- **0.80+** — good, no action needed
- **0.60-0.79** — acceptable, minor improvements
- **below 0.60** — poor, significant refactoring recommended

### Verb scoring examples

| Category | Examples | Base Score |
|----------|----------|-----------|
| Domain | `calculate`, `validate`, `reserve`, `dispatch` | 0.60 |
| Standard | `create`, `find`, `delete`, `update` | 0.45 |
| Boolean | `is`, `has`, `can`, `contains` | 1.00 |
| Generic | `get`, `set`, `process`, `handle`, `execute` | 0.10 |

### Standalone scanning (no tests needed)

```bash
./gradlew clarityScan
```

Analyzes compiled classes via reflection. Depends on `classes` task, not `test`.

### Programmatic usage

```java
var analyzer = new ClarityAnalyzer();
ClarityResult result = analyzer.analyze(traceTree);
System.out.println("Score: " + result.overallScore());
result.issues().forEach(System.out::println);

// Standalone scanning
var scanner = new ClarityScanner();
Map<String, ClarityResult> results = scanner.scan(Path.of("build/classes/java/main"));
```

---

## Architecture Decisions

### Eager serialization

All parameter values and return values are serialized to Strings at capture time (in the proxy or agent), not at render time. This means:

- The context and renderers only see Strings — never raw objects
- Template resolution (`@Narrated`, `@OnError`) happens before serialization, preserving `{param.property}` access on live objects
- No risk of objects changing state between capture and render
- `ParameterCapture.renderedValue()` and `TraceOutcome.Returned.renderedValue()` are pre-rendered Strings

### ThreadLocal-based context

The context uses `ThreadLocal<TraceStack>` rather than a shared concurrent data structure. This gives:
- Zero synchronization overhead
- Natural thread isolation
- Cross-thread propagation via explicit `ContextSnapshot`

### Sealed types and records

`TraceOutcome` is a sealed interface with `Returned` and `Threw` variants — enabling exhaustive pattern matching. All data types (`TraceNode`, `MethodSignature`, `ParameterCapture`) are records for immutability.

### Proxy at HIGHEST_PRECEDENCE in Spring

The `NarrativeTraceBeanPostProcessor` runs at `Ordered.HIGHEST_PRECEDENCE` so the tracing proxy is created first (innermost). When combined with `@EnableAsync`, the async proxy wraps outside, ensuring trace capture runs on the async thread — not the calling thread.

### Zero runtime dependencies in core

`narrativetrace-core` has zero external dependencies. All types — context, events, renderers, config — use only JDK 17 APIs. This ensures NarrativeTrace can be added to any project without dependency conflicts.

---

## Troubleshooting

### Parameters show as arg0, arg1

**Cause:** Missing `-parameters` compiler flag.

**Fix:**
```kotlin
tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}
```

Or use the Gradle plugin which adds it automatically.

### No trace output files

**Cause:** Output not enabled.

**Fix:** Add to `src/test/resources/junit-platform.properties`:
```properties
narrativetrace.output=true
```

Or run with `-Dnarrativetrace.output=true`.

### Proxy throws ClassCastException

**Cause:** Target doesn't implement the specified interface.

**Fix:** Ensure the target class implements the interface you pass to `NarrativeTraceProxy.trace()`.

### Spring beans not being traced

**Cause:** Bean's package not in `basePackages`, or bean doesn't implement any interface.

**Fix:** Add the package to `@EnableNarrativeTrace(basePackages = ...)`. NarrativeTrace uses JDK dynamic proxies, so the bean must implement at least one interface.

### Clarity score seems wrong

**Cause:** Likely using generic names that the NLP analysis flags (get/set/process/handle/data/info/temp).

**Fix:** Review the issues list in the clarity report. Replace generic names with domain-specific alternatives. Example: `getData()` → `fetchOrderHistory()`.

### Cross-thread traces are empty

**Cause:** Context not propagated to the async thread.

**Fix:** Use `ContextSnapshot`:
```java
var snapshot = context.snapshot();
executor.submit(snapshot.wrap(() -> service.process()));
```

Or register `NarrativeTraceThreadLocalAccessor` with Micrometer for automatic propagation.

### Agent doesn't instrument classes

**Cause:** Package filter doesn't match.

**Fix:** Check the `packages=` argument. Patterns support wildcards: `com.example.*` matches all subpackages. Multiple packages use semicolons: `packages=com.a.*;com.b.*`.

---

## API Quick Reference

### Essential imports

```java
// Core
import ai.narrativetrace.core.context.NarrativeContext;
import ai.narrativetrace.core.context.ThreadLocalNarrativeContext;
import ai.narrativetrace.core.config.NarrativeTraceConfig;
import ai.narrativetrace.core.config.TracingLevel;

// Proxy
import ai.narrativetrace.proxy.NarrativeTraceProxy;

// Annotations
import ai.narrativetrace.core.annotation.Narrated;
import ai.narrativetrace.core.annotation.OnError;
import ai.narrativetrace.core.annotation.NotTraced;
import ai.narrativetrace.core.annotation.NarrativeSummary;

// Renderers
import ai.narrativetrace.core.render.MarkdownRenderer;
import ai.narrativetrace.core.render.ProseRenderer;
import ai.narrativetrace.core.render.IndentedTextRenderer;

// JUnit 5
import ai.narrativetrace.junit5.NarrativeTraceExtension;

// JUnit 4
import ai.narrativetrace.junit4.NarrativeTraceRule;
import ai.narrativetrace.junit4.NarrativeTraceClassRule;

// Spring
import ai.narrativetrace.spring.EnableNarrativeTrace;

// Clarity
import ai.narrativetrace.clarity.ClarityAnalyzer;
import ai.narrativetrace.clarity.ClarityResult;
import ai.narrativetrace.clarity.ClarityScanner;

// Servlet
import ai.narrativetrace.servlet.NarrativeTraceFilter;
import ai.narrativetrace.servlet.Slf4jTraceExporter;

// SLF4J
import ai.narrativetrace.slf4j.Slf4jNarrativeContext;

// Diagrams
import ai.narrativetrace.diagrams.MermaidSequenceDiagramRenderer;
import ai.narrativetrace.diagrams.PlantUmlSequenceDiagramRenderer;
```

### Minimal working example

```java
var context = new ThreadLocalNarrativeContext();
OrderService traced = NarrativeTraceProxy.trace(new OrderServiceImpl(), OrderService.class, context);

traced.placeOrder("C-1234", 2);

System.out.println(new IndentedTextRenderer().render(context.captureTrace()));
context.reset();
```

Output:
```
OrderService.placeOrder(customerId: "C-1234", quantity: 2)
  InventoryService.reserve(productId: "SKU-KB", quantity: 2) -> Reservation[...]
  PaymentService.charge(customerId: "C-1234", amount: 179.98) -> PaymentConfirmation[...]
-> OrderResult[orderId=ORD-001, totalCharged=179.98]
```
