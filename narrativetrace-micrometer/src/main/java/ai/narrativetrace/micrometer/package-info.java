/**
 * Micrometer context-propagation bridge for automatic cross-thread trace propagation.
 *
 * <p>{@link ai.narrativetrace.micrometer.NarrativeTraceThreadLocalAccessor} implements Micrometer's
 * {@code ThreadLocalAccessor} to propagate {@code ContextSnapshot} across thread boundaries.
 * Enables automatic trace continuity with Spring Boot 3, Reactor, and {@code @Async} methods.
 */
package ai.narrativetrace.micrometer;
