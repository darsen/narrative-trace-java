package ai.narrativetrace.gradle;

import org.gradle.api.Action;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

public abstract class NarrativeTraceExtension {

    public abstract Property<Boolean> getEnabled();

    public abstract Property<Boolean> getManageDependencies();

    public abstract DirectoryProperty getOutputDir();

    public abstract Property<String> getTestFramework();

    private final ClarityExtension clarity;

    @Inject
    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod") // Gradle convention pattern
    public NarrativeTraceExtension(ObjectFactory objects) {
        this.clarity = objects.newInstance(ClarityExtension.class);
        getEnabled().convention(true);
        getManageDependencies().convention(true);
        getTestFramework().convention("junit5");
        clarity.getMinScore().convention(0.0);
        clarity.getMaxHighIssues().convention(Integer.MAX_VALUE);
        clarity.getWarnOnly().convention(false);
    }

    public ClarityExtension getClarity() {
        return clarity;
    }

    public void clarity(Action<? super ClarityExtension> action) {
        action.execute(clarity);
    }
}
