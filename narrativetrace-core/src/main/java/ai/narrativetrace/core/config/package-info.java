/**
 * Runtime configuration for trace capture behavior.
 *
 * <p>{@link ai.narrativetrace.core.config.NarrativeTraceConfig} controls the active {@link
 * ai.narrativetrace.core.config.TracingLevel} and supports volatile runtime changes. Levels range
 * from {@code OFF} (no capture) through {@code NARRATIVE} (default) to {@code DETAIL} (full
 * parameter values). {@link ai.narrativetrace.core.config.ConfigResolver} resolves configuration
 * from system properties and programmatic settings.
 */
package ai.narrativetrace.core.config;
