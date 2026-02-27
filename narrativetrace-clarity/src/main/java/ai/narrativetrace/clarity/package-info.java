/**
 * Naming clarity analysis with continuous feature-based scoring and NLP techniques.
 *
 * <p>{@link ai.narrativetrace.clarity.ClarityAnalyzer} scores trace trees on five weighted
 * dimensions: method names (0.30), class names (0.20), parameter names (0.25), structural quality
 * (0.15), and cohesion (0.10). Scores range from 0.0 to 1.0. {@link
 * ai.narrativetrace.clarity.ClarityScanner} provides standalone classpath-based analysis without
 * running tests. Supporting types include tokenizers, dictionaries, and morphology analyzers for
 * identifier decomposition.
 */
package ai.narrativetrace.clarity;
