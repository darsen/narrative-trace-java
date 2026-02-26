package ai.narrativetrace.clarity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AbbreviationDictionaryTest {

    private final AbbreviationDictionary dictionary = new AbbreviationDictionary();

    @Test
    void recognizesUniversalAbbreviation() {
        var result = dictionary.lookup("url");
        assertThat(result).isNotNull();
        assertThat(result.tier()).isEqualTo(AbbreviationDictionary.Tier.UNIVERSAL);
        assertThat(result.score()).isEqualTo(0.8);
        assertThat(result.expansion()).isEqualTo("uniform resource locator");
    }

    @Test
    void recognizesWellKnownAbbreviation() {
        var result = dictionary.lookup("ctx");
        assertThat(result).isNotNull();
        assertThat(result.tier()).isEqualTo(AbbreviationDictionary.Tier.WELL_KNOWN);
        assertThat(result.score()).isEqualTo(0.6);
        assertThat(result.expansion()).isEqualTo("context");
    }

    @Test
    void recognizesAmbiguousAbbreviation() {
        var result = dictionary.lookup("proc");
        assertThat(result).isNotNull();
        assertThat(result.tier()).isEqualTo(AbbreviationDictionary.Tier.AMBIGUOUS);
        assertThat(result.score()).isEqualTo(0.3);
    }

    @Test
    void returnsNullForFullWord() {
        var result = dictionary.lookup("calculate");
        assertThat(result).isNull();
    }

    @Test
    void isCaseInsensitive() {
        var result = dictionary.lookup("URL");
        assertThat(result).isNotNull();
        assertThat(result.tier()).isEqualTo(AbbreviationDictionary.Tier.UNIVERSAL);
    }
}
