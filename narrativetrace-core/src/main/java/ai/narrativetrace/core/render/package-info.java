/**
 * Renderers that transform trace trees into human-readable output formats.
 *
 * <p>{@link ai.narrativetrace.core.render.NarrativeRenderer} is the interface for custom output
 * formats. Built-in implementations include {@link ai.narrativetrace.core.render.MarkdownRenderer},
 * {@link ai.narrativetrace.core.render.ProseRenderer}, and {@link
 * ai.narrativetrace.core.render.IndentedTextRenderer}. {@link
 * ai.narrativetrace.core.render.ValueRenderer} handles object-to-string serialization with cycle
 * detection and POJO introspection.
 */
package ai.narrativetrace.core.render;
