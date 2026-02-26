package ai.narrativetrace.examples.ecommerce;

import ai.narrativetrace.core.annotation.NarrativeSummary;

public record Customer(String id, String name, CustomerTier tier) {

    @NarrativeSummary
    public String narrativeSummary() {
        return name + " (" + tier + ")";
    }
}
