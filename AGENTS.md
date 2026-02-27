# AI Agent Context — NarrativeTrace Java

## What This Project Is

NarrativeTrace is a Java framework that auto-generates human-readable execution traces from method names, parameter names, and return values. No log statements needed. 14 modules, ~69 public types, Java 17, Gradle Kotlin DSL, JUnit 5 + AssertJ.

Group ID: `ai.narrativetrace` (Maven Central under narrativetrace.ai domain)

## Development Workflow

- **Strict TDD baby steps**: one test at a time, red-green, run all tests after each change
- `-parameters` compiler flag is critical (preserves param names in bytecode)
- Java 17, Gradle Kotlin DSL
- `./gradlew check` runs tests + PMD + JaCoCo coverage verification + clarityCheck
- `./gradlew test` runs free-tier tests across all 14 modules
- 98% line coverage minimum (97% for agent module)

## Module Structure

```
narrativetrace-core          — zero runtime deps, records, sealed interfaces, context, renderers, config
narrativetrace-proxy         — JDK dynamic proxy, ParameterNameResolver, @NotTraced
narrativetrace-junit5        — JUnit 5 extension, auto-output, clarity reports
narrativetrace-junit4        — JUnit 4 rules (TestWatcher + ClassRule)
narrativetrace-clarity       — NLP-based naming clarity scoring (5 weighted dimensions)
narrativetrace-diagrams      — Mermaid/PlantUML sequence diagram renderers
narrativetrace-slf4j         — SLF4J bridge with MDC integration
narrativetrace-agent         — ASM 9.7.1 bytecode agent
narrativetrace-spring        — @EnableNarrativeTrace, BeanPostProcessor
narrativetrace-servlet       — Servlet filter (zero Spring deps)
narrativetrace-spring-web    — Spring auto-config for servlet filter
narrativetrace-micrometer    — Micrometer context-propagation bridge
narrativetrace-gradle-plugin — Gradle plugin: auto deps, clarityCheck, clarityScan
narrativetrace-examples      — E-commerce + Minecraft demos
```

## Key Architecture Decisions

- **Eager serialization**: Values are rendered to Strings at capture time (proxy/agent), not at render time. Context and renderers only see Strings.
- **Template resolution before serialization**: `@Narrated`/`@OnError` templates resolved from raw `Parameter[]` + `Object[] args` before serializing captures. This preserves `{param.property}` access.
- **ThreadLocal context**: `ThreadLocalNarrativeContext` uses `ThreadLocal<TraceStack>` with Deque-based call stack. Cross-thread via `ContextSnapshot`.
- **Sealed types**: `TraceOutcome` sealed with `Returned`/`Threw`. All data types are records.
- **Spring BPP at HIGHEST_PRECEDENCE**: Tracing proxy is innermost. With @Async, async proxy wraps outside → trace capture runs on async thread.

## Key Types

- `NarrativeContext` — central interface (7 methods)
- `ThreadLocalNarrativeContext` — default implementation
- `NarrativeTraceProxy` — JDK proxy creator (`trace()` overloads)
- `TraceNode` — record: signature + children + outcome + durationNanos
- `ParameterCapture` — record: (name, renderedValue, redacted) — holds pre-rendered String
- `TraceOutcome` — sealed: Returned(renderedValue) | Threw(exception)
- `ValueRenderer` — renders Objects to String (arrays, POJOs, cycle detection, toString safety)
- `TracingLevel` — enum: OFF, ERRORS, SUMMARY, NARRATIVE, DETAIL

## Testing Patterns

- Use strict asserts: `assertThatThrownBy` with `.isInstanceOf()` and `.hasMessage()`
- Tests that construct `ParameterCapture`/`TraceOutcome.Returned` directly must supply pre-rendered strings. Strings include quotes: `"\"order-42\""`. Numbers are plain: `"42"`.
- Suppressed params use empty string `""` (not null) for non-DETAIL levels.

## Build Commands

```bash
./gradlew check                                          # tests + PMD + JaCoCo + clarityCheck
./gradlew test                                           # all module tests
./gradlew :narrativetrace-gradle-plugin:functionalTest   # plugin GradleTestKit tests
./gradlew aggregateJavadoc                               # combined Javadoc HTML
./gradlew generateLlmsDocs                               # copies llms-full.md → build/site/llms-full.txt
./gradlew metricsReport                                  # NCSS size ranking
./gradlew clarityScan                                    # standalone clarity analysis
```

## Documentation

- `documentation/llms-full.md` — comprehensive reference for AI/LLM consumption
- `documentation/llms.txt` — structured index (llmstxt.org spec)
- `documentation/installation-guide.md` — setup and integration paths
- `documentation/annotations-guide.md` — all annotation usage
- `documentation/configuration-guide.md` — all config surfaces
- `documentation/clarity-guide.md` — scoring model, NLP components

## Common Gotchas

- MermaidSequenceDiagramRenderer/PlantUmlSequenceDiagramRenderer do NOT implement NarrativeRenderer — use method references (`::render`) to adapt
- Proxy uses `method.setAccessible(true)` for non-public interfaces
- Proxy must unwrap `InvocationTargetException` before recording + rethrowing
- `narrativetrace-slf4j` uses `implementation` for slf4j-api — not transitive
- Package-info.java files exist for all published module packages
- Gradle plugin uses `project.afterEvaluate` for all configuration
