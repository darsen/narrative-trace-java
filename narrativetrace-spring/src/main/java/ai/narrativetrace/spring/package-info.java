/**
 * Spring Framework integration for automatic trace capture via bean post-processing.
 *
 * <p>{@link ai.narrativetrace.spring.EnableNarrativeTrace @EnableNarrativeTrace} activates tracing
 * for Spring-managed beans. The {@link ai.narrativetrace.spring.NarrativeTraceBeanPostProcessor}
 * wraps eligible beans in tracing proxies at {@code HIGHEST_PRECEDENCE} to ensure tracing runs
 * inside any {@code @Async} proxy. Requires Spring Context 6.2+.
 */
package ai.narrativetrace.spring;
