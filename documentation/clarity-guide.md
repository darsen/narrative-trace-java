# NarrativeTrace Java Clarity Guide

If the trace is the code, then trace quality is code quality. The clarity module analyzes your method, class, and parameter names and scores how well they communicate intent.

## Quick start

```java
var analyzer = new ClarityAnalyzer();
var result = analyzer.analyze(context.captureTrace());

var renderer = new ClarityReportRenderer();
System.out.println(renderer.render("Order Placement", result));
```

With JUnit 5, clarity reports are generated automatically when `narrativetrace.output=true` — no code needed.

## What gets scored

Clarity produces a single overall score (0.0–1.0) from five weighted components:

| Component | Weight | What it measures |
|---|---|---|
| Method names | 30% | Verb quality, token specificity, abbreviations, token count |
| Parameter names | 25% | Domain specificity vs generic/meaningless tokens |
| Class names | 20% | Role suffix quality, prefix specificity |
| Structural | 15% | Parameter count and call depth penalties |
| Cohesion | 10% | Whether methods align with the class's role suffix |

## Scoring in practice

### Method names

The first token is treated as a verb. Domain verbs score highest, generic verbs score lowest:

| Verb category | Examples | Score |
|---|---|---|
| Domain | `calculate`, `validate`, `reserve`, `dispatch` | 0.60 |
| Standard | `create`, `find`, `delete`, `update` | 0.45 |
| Boolean prefix | `is`, `has`, `can`, `contains` | 1.00 |
| Generic | `get`, `set`, `process`, `handle`, `execute` | 0.10 |

Multi-token methods like `reserveInventory` score higher than single-token methods like `reserve` because the additional tokens add specificity.

### Class names

A role suffix is expected. Design pattern and functional suffixes score well when paired with a domain prefix:

| Pattern | Score | Why |
|---|---|---|
| `OrderService` | 1.0 | Domain prefix + functional suffix |
| `Service` | 0.0 | No prefix — meaningless |
| `DataProcessor` | Low | Vague prefix + generic suffix |
| `BookingManager` | Medium | Domain prefix, but `Manager` is generic |

### Parameter names

Domain-specific names score high, generic names score low:

| Tier | Examples | Score |
|---|---|---|
| Domain-specific | `customerId`, `checkInDate`, `roomCategory` | 0.80+ |
| Typed generic | `id`, `name`, `count`, `status` | 0.50 |
| Vague | `data`, `info`, `result`, `object` | 0.10 |
| Meaningless | `x`, `foo`, `val`, `temp` | 0.00 |

### Structural penalties

Methods with more than 4 parameters or call depth beyond 5 are penalized. Each excess parameter costs 0.1; each excess depth level costs 0.05.

### Cohesion

Methods are checked against expected verbs for the class's role suffix. A `Repository` class is expected to have methods like `find`, `save`, `delete`, `count`. A method like `renderReport` on a `GuestRepository` is flagged as misaligned.

## Issues and severity

Clarity problems are reported as issues ranked by impact:

| Severity | Threshold | Examples |
|---|---|---|
| HIGH | score ≤ 0.20 | `DataProcessor.execute(data)` |
| MEDIUM | score ≤ 0.50 | `BookingManager.handleBooking(name, type)` |
| LOW | score > 0.50 | Minor abbreviation use |

Duplicate issues (same category and element) are deduplicated with an occurrence count. Issues are ranked by impact score (severity weight × occurrences).

## Report output

### Single scenario

```java
renderer.render("Guest books a room", result);
```

Produces a Markdown report with a scores table and an issues table (if any):

```markdown
## Clarity Report — Guest books a room
Overall: 0.95 (high)

| Component  | Score |
|------------|-------|
| Method     | 0.98  |
| Class      | 1.00  |
| Parameter  | 0.90  |
| Structural | 1.00  |
| Cohesion   | 0.85  |
```

### Suite report

```java
renderer.renderSuiteReport(Map.of(
    "Guest books a room", result1,
    "Legacy data processing", result2
));
```

Produces a ranked summary of all scenarios. Scenarios scoring below 0.7 get a detailed breakdown with individual issues.

## JUnit 5 integration

When `narrativetrace.output=true` in `junit-platform.properties`, the JUnit extension automatically:

1. Runs `ClarityAnalyzer.analyze()` on each test's trace
2. Writes `clarity-report.md` to the output directory after all tests complete
3. Prints a console summary with score distribution:

```
NarrativeTrace — Suite complete
  2 scenarios recorded
  Clarity: 100% high | 0% moderate | 0% low
  Reports: build/narrativetrace
```

No code changes needed — just enable output and run your tests.

## Build enforcement with `clarityCheck`

The Gradle plugin provides a `clarityCheck` task that fails the build when naming quality drops below a threshold. This makes clarity scoring enforceable, not advisory.

### Setup

```kotlin
plugins {
    id("ai.narrativetrace") version "0.1.0-SNAPSHOT"
}

narrativeTrace {
    clarity {
        minScore.set(0.80)     // fail if any scenario scores below 0.80
        maxHighIssues.set(0)   // fail if any scenario has HIGH-severity issues
    }
}
```

### How it works

1. `./gradlew test` — the JUnit extension produces `build/narrativetrace/clarity-results.json`
2. `clarityCheck` reads the JSON and compares each scenario against thresholds
3. `./gradlew check` runs both `test` and `clarityCheck` automatically

### Failure output

When a scenario falls below the threshold:

```
FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':clarityCheck'.
> Clarity check failed:
    'Legacy data processing': score 0.45 < threshold 0.80
    'Legacy data processing': 3 HIGH issues (max 0)
```

### Warn-only mode

For gradual adoption, use `warnOnly` to log violations without failing the build:

```kotlin
narrativeTrace {
    clarity {
        minScore.set(0.70)
        warnOnly.set(true)
    }
}
```

### JSON contract

The `clarity-results.json` file is the contract between test execution and the `clarityCheck` task:

```json
{
  "version": "1.0",
  "scenarios": [
    {
      "name": "Customer places order",
      "overallScore": 0.85,
      "methodNameScore": 0.90,
      "classNameScore": 0.95,
      "parameterNameScore": 0.80,
      "structuralScore": 1.00,
      "cohesionScore": 0.70,
      "issues": [
        {
          "category": "param-name",
          "element": "data",
          "suggestion": "Use a domain-specific name",
          "severity": "MEDIUM",
          "occurrences": 2,
          "impactScore": 4.0
        }
      ]
    }
  ]
}
```

## Demo

Run the hotel booking clarity demo to see scoring across four quality levels:

```bash
./gradlew :narrativetrace-examples:runClarity
```

The demo traces four scenarios with progressively worse naming — from `ReservationService.confirmReservation(guestId, roomCategory)` (excellent) down to `DataProcessor.execute(data, val)` (poor) — and generates a suite clarity report showing the score differences.

## NLP components

The clarity module uses hand-coded NLP with no external dependencies:

| Component | Purpose |
|---|---|
| `IdentifierTokenizer` | Splits camelCase and snake_case into tokens |
| `VerbDictionary` | Categorizes 200+ verbs (domain, standard, generic, boolean) |
| `RoleSuffixDictionary` | Classifies class suffixes (design pattern, functional, generic) |
| `GenericTokenDetector` | Ranks token specificity (meaningless → domain-specific) |
| `AbbreviationDictionary` | Scores 140+ abbreviations in three tiers (universal, well-known, ambiguous) |
| `MorphologyAnalyzer` | Detects parts of speech via suffixes (-tion, -ize, -able) |
| `CohesionScorer` | Checks method-verb alignment with class role expectations |

## See also

- [Configuration Guide](configuration-guide.md) — tracing levels, output configuration
- [Annotations Guide](annotations-guide.md) — `@Narrated`, `@OnError`, `@NotTraced`
- [Installation Guide](installation-guide.md) — dependencies and integration paths
