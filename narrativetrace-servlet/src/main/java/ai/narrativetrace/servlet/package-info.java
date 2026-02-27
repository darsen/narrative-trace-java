/**
 * Servlet filter for production request lifecycle trace capture (zero Spring dependencies).
 *
 * <p>{@link ai.narrativetrace.servlet.NarrativeTraceFilter} follows a
 * reset-chain-capture-export-reset lifecycle on each HTTP request. {@link
 * ai.narrativetrace.servlet.Slf4jTraceExporter} exports captured traces as JSON via SLF4J at INFO
 * level.
 */
package ai.narrativetrace.servlet;
