/**
 * Export SPI for production request lifecycle integration.
 *
 * <p>{@link ai.narrativetrace.core.export.TraceExporter} is the interface for exporting captured
 * traces at request boundaries (e.g., servlet filters, message handlers). {@link
 * ai.narrativetrace.core.export.JsonExporter} serializes trace trees to JSON. {@link
 * ai.narrativetrace.core.export.RequestContext} carries request metadata for correlation.
 */
package ai.narrativetrace.core.export;
