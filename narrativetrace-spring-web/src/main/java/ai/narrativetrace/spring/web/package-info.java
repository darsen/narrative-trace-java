/**
 * Spring Web integration that auto-configures the servlet trace filter.
 *
 * <p>{@link ai.narrativetrace.spring.web.NarrativeTraceWebConfiguration} registers the {@link
 * ai.narrativetrace.servlet.NarrativeTraceFilter} as a Spring bean with pluggable {@link
 * ai.narrativetrace.core.export.TraceExporter} via {@code ObjectProvider}.
 */
package ai.narrativetrace.spring.web;
