/**
 * JUnit 4 rules for automatic trace output during test execution.
 *
 * <p>{@link ai.narrativetrace.junit4.NarrativeTraceRule} is a per-test {@code TestWatcher} that
 * captures traces for individual test methods. {@link
 * ai.narrativetrace.junit4.NarrativeTraceClassRule} is a class-level {@code TestRule} that
 * accumulates traces across all tests in a class and produces combined output including clarity
 * reports.
 */
package ai.narrativetrace.junit4;
