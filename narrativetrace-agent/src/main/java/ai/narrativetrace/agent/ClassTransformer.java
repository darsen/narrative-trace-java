package ai.narrativetrace.agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/** Orchestrates ASM class transformation by coordinating visitors and metadata collection. */
public final class ClassTransformer {

  private ClassTransformer() {}

  public static byte[] transform(byte[] classfileBuffer, String className) {
    var reader = new ClassReader(classfileBuffer);

    // Pass 1: collect metadata (parameter names, annotations)
    var collector = new MethodMetadataCollector();
    reader.accept(collector, 0);

    // Pass 2: transform with metadata
    var writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
    var visitor = new NarrativeClassVisitor(writer, className, collector.getMetadata());
    reader.accept(visitor, ClassReader.EXPAND_FRAMES);
    return writer.toByteArray();
  }
}
