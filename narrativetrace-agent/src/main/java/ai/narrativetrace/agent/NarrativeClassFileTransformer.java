package ai.narrativetrace.agent;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public final class NarrativeClassFileTransformer implements ClassFileTransformer {

    private final AgentConfig config;

    public NarrativeClassFileTransformer(AgentConfig config) {
        this.config = config;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) {
        if (className == null || !config.shouldTransform(className)) {
            return null; // no transformation
        }

        return ClassTransformer.transform(classfileBuffer, className);
    }
}
