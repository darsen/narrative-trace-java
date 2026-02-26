package ai.narrativetrace.clarity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MorphologyAnalyzerTest {

    private final MorphologyAnalyzer analyzer = new MorphologyAnalyzer();

    @Test
    void detectsVerbBySuffix() {
        assertThat(analyzer.analyze("validate").partOfSpeech()).isEqualTo(MorphologyAnalyzer.PartOfSpeech.VERB);
        assertThat(analyzer.analyze("normalize").partOfSpeech()).isEqualTo(MorphologyAnalyzer.PartOfSpeech.VERB);
        assertThat(analyzer.analyze("notify").partOfSpeech()).isEqualTo(MorphologyAnalyzer.PartOfSpeech.VERB);
        assertThat(analyzer.analyze("flatten").partOfSpeech()).isEqualTo(MorphologyAnalyzer.PartOfSpeech.VERB);
    }

    @Test
    void detectsNounBySuffix() {
        assertThat(analyzer.analyze("transaction").partOfSpeech()).isEqualTo(MorphologyAnalyzer.PartOfSpeech.NOUN);
        assertThat(analyzer.analyze("payment").partOfSpeech()).isEqualTo(MorphologyAnalyzer.PartOfSpeech.NOUN);
        assertThat(analyzer.analyze("security").partOfSpeech()).isEqualTo(MorphologyAnalyzer.PartOfSpeech.NOUN);
        assertThat(analyzer.analyze("performance").partOfSpeech()).isEqualTo(MorphologyAnalyzer.PartOfSpeech.NOUN);
        assertThat(analyzer.analyze("handler").partOfSpeech()).isEqualTo(MorphologyAnalyzer.PartOfSpeech.NOUN);
        assertThat(analyzer.analyze("repository").partOfSpeech()).isEqualTo(MorphologyAnalyzer.PartOfSpeech.NOUN);
    }

    @Test
    void detectsAdjectiveBySuffix() {
        assertThat(analyzer.analyze("readable").partOfSpeech()).isEqualTo(MorphologyAnalyzer.PartOfSpeech.ADJECTIVE);
        assertThat(analyzer.analyze("active").partOfSpeech()).isEqualTo(MorphologyAnalyzer.PartOfSpeech.ADJECTIVE);
        assertThat(analyzer.analyze("synchronous").partOfSpeech()).isEqualTo(MorphologyAnalyzer.PartOfSpeech.ADJECTIVE);
        assertThat(analyzer.analyze("optional").partOfSpeech()).isEqualTo(MorphologyAnalyzer.PartOfSpeech.ADJECTIVE);
        assertThat(analyzer.analyze("dynamic").partOfSpeech()).isEqualTo(MorphologyAnalyzer.PartOfSpeech.ADJECTIVE);
    }

    @Test
    void returnsUnknownForAmbiguous() {
        assertThat(analyzer.analyze("cart").partOfSpeech()).isEqualTo(MorphologyAnalyzer.PartOfSpeech.UNKNOWN);
        assertThat(analyzer.analyze("shop").partOfSpeech()).isEqualTo(MorphologyAnalyzer.PartOfSpeech.UNKNOWN);
    }

    @Test
    void handlesShortWords() {
        assertThat(analyzer.analyze("up").partOfSpeech()).isEqualTo(MorphologyAnalyzer.PartOfSpeech.UNKNOWN);
        assertThat(analyzer.analyze("in").partOfSpeech()).isEqualTo(MorphologyAnalyzer.PartOfSpeech.UNKNOWN);
    }

    @Test
    void prefersVerbDictionaryOverSuffix() {
        // "action" ends with -tion (noun suffix) but if it were in VerbDictionary, verb wins.
        // Since "action" is NOT in VerbDictionary, it stays NOUN by suffix.
        assertThat(analyzer.analyze("action").partOfSpeech()).isEqualTo(MorphologyAnalyzer.PartOfSpeech.NOUN);

        // "calculate" is a verb by both dictionary and suffix — should be VERB
        assertThat(analyzer.analyze("calculate").partOfSpeech()).isEqualTo(MorphologyAnalyzer.PartOfSpeech.VERB);
    }

    @Test
    void detectsVerbBySuffixWhenNotInDictionary() {
        // "customize" ends in -ize, not in VerbDictionary, but morphologically a verb
        assertThat(analyzer.analyze("customize").partOfSpeech()).isEqualTo(MorphologyAnalyzer.PartOfSpeech.VERB);
    }
}
