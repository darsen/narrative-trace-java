# NarrativeTrace Java Installation Guide

This guide covers installing and wiring NarrativeTrace Java in a JVM project.

## Prerequisites

- Java 17+
- Gradle build

## Quick Start with Gradle Plugin

The Gradle plugin handles all wiring automatically — dependencies, compiler flags, and test JVM configuration:

```kotlin
plugins {
    id("ai.narrativetrace") version "0.1.0-SNAPSHOT"
}
```

That's it. Run `./gradlew test` and trace output appears in `build/narrativetrace/`.

To enforce naming quality thresholds:

```kotlin
narrativeTrace {
    clarity {
        minScore.set(0.80)
        maxHighIssues.set(0)
    }
}
```

Now `./gradlew check` fails if any scenario's clarity score drops below 0.80 or has any HIGH-severity issues.

See [Configuration Guide — Gradle Plugin DSL](configuration-guide.md#gradle-plugin-dsl) for the full reference.

## Manual Setup

The sections below cover manual installation for projects that don't use the Gradle plugin.

### Prerequisites

- Compiler parameter metadata enabled (`-parameters`)

## 1. Enable Parameter Name Retention

NarrativeTrace uses method parameter names in trace output. Without `-parameters`, traces show `arg0`, `arg1`, etc.

```kotlin
// build.gradle.kts
tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}
```

## 2. Add Dependencies

Start with the minimum stack and then add only the integrations you need.

```kotlin
dependencies {
    // Minimum
    implementation("ai.narrativetrace:narrativetrace-core:0.1.0-SNAPSHOT")
    implementation("ai.narrativetrace:narrativetrace-proxy:0.1.0-SNAPSHOT")

    // Optional integrations
    testImplementation("ai.narrativetrace:narrativetrace-junit5:0.1.0-SNAPSHOT")
    testImplementation("ai.narrativetrace:narrativetrace-junit4:0.1.0-SNAPSHOT")  // for JUnit 4
    implementation("ai.narrativetrace:narrativetrace-spring:0.1.0-SNAPSHOT")
    implementation("ai.narrativetrace:narrativetrace-slf4j:0.1.0-SNAPSHOT")
    implementation("ai.narrativetrace:narrativetrace-diagrams:0.1.0-SNAPSHOT")
    implementation("ai.narrativetrace:narrativetrace-clarity:0.1.0-SNAPSHOT")
    implementation("ai.narrativetrace:narrativetrace-micrometer:0.1.0-SNAPSHOT")
    implementation("ai.narrativetrace:narrativetrace-agent:0.1.0-SNAPSHOT")
    implementation("ai.narrativetrace:narrativetrace-servlet:0.1.0-SNAPSHOT")
    implementation("ai.narrativetrace:narrativetrace-spring-web:0.1.0-SNAPSHOT")
}
```

## 3. Choose an Integration Path

### Option A: JDK Proxy (works in any Java app)

```java
var context = new ThreadLocalNarrativeContext();
var tracedOrderService = NarrativeTraceProxy.trace(orderService, OrderService.class, context);

tracedOrderService.placeOrder("C-1234", "SKU-KB", 2);
System.out.println(new IndentedTextRenderer().render(context.captureTrace()));
context.reset();
```

Use this when services are interface-based.

### Option B: Spring Auto-Wrapping

```java
@Configuration
@EnableNarrativeTrace(basePackages = {"com.example.myapp"})
public class AppConfig {}
```

Use this when you want bean post-processing to wrap eligible beans automatically.

### Option C: JUnit 5 Auto Context + Trace Output

```java
@ExtendWith(NarrativeTraceExtension.class)
class OrderServiceTest {
    @Test
    void customerPlacesOrder(NarrativeContext context) {
        var orderService = NarrativeTraceProxy.trace(new DefaultOrderService(), OrderService.class, context);
        orderService.placeOrder("C-1234", "SKU-KB", 2);
    }
}
```

Features:
- Per-test `NarrativeContext` via parameter injection
- Automatic failure trace printing to console
- Scenario name derived from test method name (`customerPlacesOrder` → "Customer places order")
- With `narrativetrace.output=true`: writes `.md`, `.json`, `.mmd` per test, plus a suite-level `clarity-report.md`

### Option D: JUnit 4 Auto Context + Trace Output

```java
public class OrderServiceTest {
    @Rule
    public NarrativeTraceRule narrativeTrace = new NarrativeTraceRule();

    @Test
    public void customerPlacesOrder() {
        NarrativeContext context = narrativeTrace.context();
        var orderService = NarrativeTraceProxy.trace(new DefaultOrderService(), OrderService.class, context);
        orderService.placeOrder("C-1234", "SKU-KB", 2);
    }
}
```

Features:
- Per-test `NarrativeContext` via `narrativeTrace.context()`
- Automatic failure trace printing to console
- Scenario name derived from test method name (`customerPlacesOrder` → "Customer places order")
- With `-Dnarrativetrace.output=true`: writes `.md`, `.json`, `.mmd` per test
- Add `@ClassRule` with `NarrativeTraceClassRule` for suite-level `clarity-report.md` and console summary

Configuration uses system properties (JUnit 4 has no `junit-platform.properties`):
- `narrativetrace.output` — `true`/`false` (default: `false`)
- `narrativetrace.outputDir` — path (default: `build/narrativetrace`)
- `narrativetrace.format` — `markdown`/`text`/`mermaid`/`plantuml` (default: `markdown`)

### Option E: Java Agent (no proxy wiring)

```bash
java -javaagent:narrativetrace-agent.jar=packages=com.example.myapp.* -jar your-app.jar
```

Use this when you want bytecode instrumentation for classes under selected package prefixes. When no CLI args are provided, the agent falls back to `narrativetrace.properties` on the classpath.

Agent argument format: `packages=<pkg1>;<pkg2>;...`

Package patterns support wildcards:

| Pattern | Matches |
|---|---|
| `com.example.*` | All classes under `com.example` and subpackages |
| `com.example.**` | Same as `.*` (both match all subpackages) |
| `com.example` | Same as `com.example.*` (bare prefix with boundary enforcement) |

Multiple packages:

```bash
java -javaagent:narrativetrace-agent.jar=packages=com.example.app.*;com.example.shared.* -jar app.jar
```

Package separators are semicolons (`;`), not commas. Unknown keys are ignored; duplicate keys are rejected.

## 4. Configure Trace Output

### JUnit 5 (recommended): `junit-platform.properties`

Add `src/test/resources/junit-platform.properties`:

```properties
narrativetrace.output=true
narrativetrace.format=markdown
```

No Gradle wiring needed. This file is test-only and never touches production.

### Gradle: `gradle.properties` (alternative)

Define trace output settings in one place:

```properties
# gradle.properties
narrativetrace.output=true
narrativetrace.format=markdown
```

Then forward to the test JVM in `build.gradle.kts`:

```kotlin
tasks.withType<Test> {
    useJUnitPlatform()
    listOf("narrativetrace.output", "narrativetrace.outputDir", "narrativetrace.format")
        .forEach { key ->
            (findProperty(key) as? String)?.let { systemProperty(key, it) }
        }
}
```

### CLI override

System properties override all other sources:

```bash
./gradlew test -Dnarrativetrace.output=true
./gradlew test -Pnarrativetrace.format=text
```

### Pure Java / Agent: `narrativetrace.properties`

Add a file to the classpath (e.g. `src/main/resources/narrativetrace.properties`):

```properties
narrativetrace.packages=com.example.app.*;com.example.shared.*
```

## 5. Validate Installation

Run tests:

```bash
./gradlew test
```

If `junit-platform.properties` has `narrativetrace.output=true`, trace files are written automatically.

Expected output structure:

```
build/narrativetrace/
├── traces/
│   └── OrderServiceTest/
│       ├── customer_places_order.md
│       ├── customer_places_order.json
│       └── ...
├── diagrams/
│   └── OrderServiceTest/
│       ├── customer_places_order.mmd
│       └── ...
└── clarity-report.md
```

## Module Selection Reference

| Module | When to add it |
|---|---|
| `narrativetrace-core` | Always required |
| `narrativetrace-proxy` | Interface-based tracing via JDK proxies |
| `narrativetrace-junit5` | JUnit 5 extension and trace file emission |
| `narrativetrace-junit4` | JUnit 4 rule and class rule for trace output |
| `narrativetrace-spring` | Spring bean auto-wrapping via `@EnableNarrativeTrace` |
| `narrativetrace-slf4j` | Emit narrative events to SLF4J logger |
| `narrativetrace-diagrams` | Mermaid / PlantUML renderers |
| `narrativetrace-clarity` | Naming clarity analysis and reporting |
| `narrativetrace-micrometer` | Cross-thread trace propagation via Micrometer context-propagation |
| `narrativetrace-agent` | Java agent instrumentation |
| `narrativetrace-servlet` | Production servlet filter — per-request trace lifecycle and export (no Spring) |
| `narrativetrace-spring-web` | Spring `@Configuration` auto-wiring the servlet filter with pluggable exporter |

## See also

- [Configuration Guide](configuration-guide.md) — tracing levels, JUnit/Gradle/Spring/SLF4J configuration
- [Annotations Guide](annotations-guide.md) — `@Narrated`, `@OnError`, `@NotTraced`, `@NarrativeSummary`, `@EnableNarrativeTrace`
- [Clarity Guide](clarity-guide.md) — scoring model, NLP components, JUnit integration
