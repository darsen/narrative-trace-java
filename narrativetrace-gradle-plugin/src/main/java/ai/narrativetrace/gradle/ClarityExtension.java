package ai.narrativetrace.gradle;

import org.gradle.api.provider.Property;

public abstract class ClarityExtension {

    public abstract Property<Double> getMinScore();

    public abstract Property<Integer> getMaxHighIssues();

    public abstract Property<Boolean> getWarnOnly();
}
