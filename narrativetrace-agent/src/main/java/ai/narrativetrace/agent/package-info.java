/**
 * Java agent for bytecode-level trace instrumentation via ASM.
 *
 * <p>{@link ai.narrativetrace.agent.NarrativeTraceAgent} is the agent entry point ({@code
 * premain}). {@link ai.narrativetrace.agent.NarrativeClassFileTransformer} selects classes for
 * instrumentation based on package filters. {@link ai.narrativetrace.agent.NarrativeMethodVisitor}
 * injects trace capture calls using ASM's {@code AdviceAdapter} with try-catch-rethrow for
 * exception tracking. Requires ASM 9.7+.
 */
package ai.narrativetrace.agent;
