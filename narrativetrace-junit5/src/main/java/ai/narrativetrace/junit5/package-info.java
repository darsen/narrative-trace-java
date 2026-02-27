/**
 * JUnit 5 extension for automatic trace output during test execution.
 *
 * <p>{@link ai.narrativetrace.junit5.NarrativeTraceExtension} implements {@code
 * BeforeEachCallback}, {@code AfterTestExecutionCallback}, and {@code AfterAllCallback} to
 * automatically capture traces and produce Markdown, Mermaid diagrams, JSON, and clarity reports
 * per test class. Register via {@code @ExtendWith(NarrativeTraceExtension.class)}.
 */
package ai.narrativetrace.junit5;
