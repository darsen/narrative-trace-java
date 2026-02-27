package ai.narrativetrace.agent;

import java.util.Map;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** ASM ClassVisitor that delegates to {@link NarrativeMethodVisitor} for eligible methods. */
public final class NarrativeClassVisitor extends ClassVisitor {

  private final String className;
  private final Map<String, MethodMetadata> metadata;

  public NarrativeClassVisitor(
      ClassVisitor cv, String className, Map<String, MethodMetadata> metadata) {
    super(Opcodes.ASM9, cv);
    this.className = className;
    this.metadata = metadata;
  }

  @Override
  public MethodVisitor visitMethod(
      int access, String name, String descriptor, String signature, String[] exceptions) {
    var mv = super.visitMethod(access, name, descriptor, signature, exceptions);
    if (name.equals("<init>") || name.equals("<clinit>")) {
      return mv;
    }
    var methodMetadata = metadata.get(MethodMetadataCollector.key(name, descriptor));
    return new NarrativeMethodVisitor(mv, access, name, descriptor, className, methodMetadata);
  }
}
