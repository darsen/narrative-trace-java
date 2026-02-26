package ai.narrativetrace.clarity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GenericTokenDetectorTest {

    private final GenericTokenDetector detector = new GenericTokenDetector();

    @Test
    void detectsMeaninglessSingleLetter() {
        var result = detector.detect("x");
        assertThat(result.tier()).isEqualTo(GenericTokenDetector.Tier.MEANINGLESS);
        assertThat(result.score()).isEqualTo(0.0);
    }

    @Test
    void detectsMeaninglessPlaceholder() {
        var result = detector.detect("foo");
        assertThat(result.tier()).isEqualTo(GenericTokenDetector.Tier.MEANINGLESS);
        assertThat(result.score()).isEqualTo(0.0);
    }

    @Test
    void detectsVagueWord() {
        var result = detector.detect("data");
        assertThat(result.tier()).isEqualTo(GenericTokenDetector.Tier.VAGUE);
        assertThat(result.score()).isEqualTo(0.2);
    }

    @Test
    void detectsTypedGeneric() {
        var result = detector.detect("count");
        assertThat(result.tier()).isEqualTo(GenericTokenDetector.Tier.TYPED_GENERIC);
        assertThat(result.score()).isEqualTo(0.5);
    }

    @Test
    void returnsNotGenericForDomainWord() {
        var result = detector.detect("customer");
        assertThat(result.tier()).isEqualTo(GenericTokenDetector.Tier.NOT_GENERIC);
        assertThat(result.score()).isEqualTo(1.0);
    }

    @Test
    void isCaseInsensitive() {
        var result = detector.detect("Data");
        assertThat(result.tier()).isEqualTo(GenericTokenDetector.Tier.VAGUE);
        assertThat(result.score()).isEqualTo(0.2);
    }
}
