package ai.narrativetrace.clarity;

import static org.assertj.core.api.Assertions.assertThat;

import ai.narrativetrace.core.event.MethodSignature;
import ai.narrativetrace.core.event.TraceNode;
import ai.narrativetrace.core.event.TraceOutcome;
import ai.narrativetrace.core.tree.DefaultTraceTree;
import java.util.List;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;

class ClarityAnalyzerPropertyTest {

  private final ClarityAnalyzer analyzer = new ClarityAnalyzer();

  @Property
  void allScoresAreInUnitRange(
      @ForAll @AlphaChars @StringLength(min = 1, max = 30) String className,
      @ForAll @AlphaChars @StringLength(min = 1, max = 30) String methodName) {
    var sig = new MethodSignature(className, methodName, List.of());
    var node = new TraceNode(sig, List.of(), new TraceOutcome.Returned("ok"));
    var tree = new DefaultTraceTree(List.of(node));

    var result = analyzer.analyze(tree);

    assertThat(result.overallScore()).isBetween(0.0, 1.0);
    assertThat(result.methodNameScore()).isBetween(0.0, 1.0);
    assertThat(result.classNameScore()).isBetween(0.0, 1.0);
    assertThat(result.parameterNameScore()).isBetween(0.0, 1.0);
    assertThat(result.structuralScore()).isBetween(0.0, 1.0);
    assertThat(result.cohesionScore()).isBetween(0.0, 1.0);
  }

  @Property
  void sameInputProducesIdenticalResult(
      @ForAll @AlphaChars @StringLength(min = 1, max = 20) String className,
      @ForAll @AlphaChars @StringLength(min = 1, max = 20) String methodName) {
    var sig = new MethodSignature(className, methodName, List.of());
    var node = new TraceNode(sig, List.of(), new TraceOutcome.Returned("ok"));
    var tree = new DefaultTraceTree(List.of(node));

    var first = analyzer.analyze(tree);
    var second = analyzer.analyze(tree);

    assertThat(first.overallScore()).isEqualTo(second.overallScore());
    assertThat(first.methodNameScore()).isEqualTo(second.methodNameScore());
    assertThat(first.classNameScore()).isEqualTo(second.classNameScore());
    assertThat(first.parameterNameScore()).isEqualTo(second.parameterNameScore());
    assertThat(first.structuralScore()).isEqualTo(second.structuralScore());
    assertThat(first.cohesionScore()).isEqualTo(second.cohesionScore());
  }
}
