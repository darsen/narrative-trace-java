package ai.narrativetrace.clarity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ParameterNameScorerTest {

    private final ParameterNameScorer scorer = new ParameterNameScorer();

    @Test
    void scoresDomainSpecificParamHighly() {
        assertThat(scorer.score("customerId")).isGreaterThanOrEqualTo(0.85);
        assertThat(scorer.score("orderTotal")).isGreaterThanOrEqualTo(0.85);
        assertThat(scorer.score("shippingAddress")).isGreaterThanOrEqualTo(0.85);
    }

    @Test
    void scoresMeaninglessParamLow() {
        assertThat(scorer.score("x")).isLessThanOrEqualTo(0.05);
        assertThat(scorer.score("data")).isLessThanOrEqualTo(0.20);
        assertThat(scorer.score("obj")).isLessThanOrEqualTo(0.20);
        assertThat(scorer.score("tmp")).isLessThanOrEqualTo(0.20);
    }

    @Test
    void scoresTypedGenericMedium() {
        double count = scorer.score("count");
        assertThat(count).isBetween(0.40, 0.60);

        double name = scorer.score("name");
        assertThat(name).isBetween(0.40, 0.60);
    }

    @Test
    void penalizesAbbreviatedParam() {
        double abbreviated = scorer.score("custId");
        double full = scorer.score("customerId");
        assertThat(abbreviated).isLessThan(full);
    }

    @Test
    void scoresCompoundDomainParamHighly() {
        assertThat(scorer.score("shippingAddress")).isGreaterThanOrEqualTo(0.90);
        assertThat(scorer.score("accountBalance")).isGreaterThanOrEqualTo(0.85);
    }

    @Test
    void scoresNotGenericAbbreviatedSingleToken() {
        // "ctx" is NOT_GENERIC in GenericTokenDetector but is a known abbreviation
        double ctx = scorer.score("ctx");
        assertThat(ctx).isBetween(0.30, 0.50);
    }

    @Test
    void penalizesAmbiguousAbbreviationInCompound() {
        // "procId" has ambiguous abbreviation "proc"
        double procId = scorer.score("procId");
        double processId = scorer.score("processId");
        assertThat(procId).isLessThan(processId);
    }
}
