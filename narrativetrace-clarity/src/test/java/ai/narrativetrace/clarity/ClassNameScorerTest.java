package ai.narrativetrace.clarity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClassNameScorerTest {

    private final ClassNameScorer scorer = new ClassNameScorer();

    @Test
    void scoresDomainPlusPatternSuffixHighly() {
        assertThat(scorer.score("OrderService")).isGreaterThanOrEqualTo(0.90);
        assertThat(scorer.score("PaymentGateway")).isGreaterThanOrEqualTo(0.90);
        assertThat(scorer.score("UserRepository")).isGreaterThanOrEqualTo(0.90);
    }

    @Test
    void scoresBareGenericSuffixLow() {
        assertThat(scorer.score("Manager")).isLessThanOrEqualTo(0.25);
        assertThat(scorer.score("Helper")).isLessThanOrEqualTo(0.25);
        assertThat(scorer.score("Util")).isLessThanOrEqualTo(0.25);
    }

    @Test
    void scoresGenericSuffixWithPrefixMedium() {
        double orderManager = scorer.score("OrderManager");
        assertThat(orderManager).isBetween(0.40, 0.60);

        double dataProcessor = scorer.score("DataProcessor");
        assertThat(dataProcessor).isBetween(0.25, 0.50);
    }

    @Test
    void scoresDomainMultiWordHighly() {
        assertThat(scorer.score("ShoppingCart")).isGreaterThanOrEqualTo(0.85);
        assertThat(scorer.score("PricingEngine")).isGreaterThanOrEqualTo(0.85);
    }

    @Test
    void penalizesAbbreviatedPrefix() {
        double abbreviated = scorer.score("OrdSvc");
        double full = scorer.score("OrderService");
        assertThat(abbreviated).isLessThan(full);
    }

    @Test
    void scoresInterfaceAdjectivePattern() {
        assertThat(scorer.score("Comparable")).isGreaterThanOrEqualTo(0.80);
        assertThat(scorer.score("Serializable")).isGreaterThanOrEqualTo(0.80);
    }

    @Test
    void scoresMeaninglessPrefixWithPatternSuffix() {
        // "DataService" - vague prefix + good suffix → scores lower than domain prefix
        double dataService = scorer.score("DataService");
        double orderService = scorer.score("OrderService");
        assertThat(dataService).isLessThan(orderService);
    }

    @Test
    void scoresVerbSuffixLow() {
        // "OrderValidate" - verb as class suffix
        double score = scorer.score("OrderValidate");
        assertThat(score).isLessThan(scorer.score("OrderValidator"));
    }

    @Test
    void penalizesExcessiveTokenCount() {
        double longName = scorer.score("VeryLongMultiWordServiceName");
        assertThat(longName).isLessThan(scorer.score("OrderService"));
    }
}
