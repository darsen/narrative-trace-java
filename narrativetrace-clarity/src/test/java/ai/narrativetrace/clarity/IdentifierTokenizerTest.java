package ai.narrativetrace.clarity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IdentifierTokenizerTest {

    private final IdentifierTokenizer tokenizer = new IdentifierTokenizer();

    @Test
    void splitsSimpleCamelCase() {
        assertThat(tokenizer.tokenize("calculateTotal")).containsExactly("calculate", "total");
    }

    @Test
    void splitsAbbreviationBoundary() {
        assertThat(tokenizer.tokenize("HTTPSConnection")).containsExactly("https", "connection");
    }

    @Test
    void splitsAlphaDigitBoundary() {
        assertThat(tokenizer.tokenize("name2")).containsExactly("name", "2");
    }

    @Test
    void splitsDigitAlphaBoundary() {
        assertThat(tokenizer.tokenize("2name")).containsExactly("2", "name");
    }

    @Test
    void splitsSnakeCase() {
        assertThat(tokenizer.tokenize("parse_xml_document")).containsExactly("parse", "xml", "document");
    }

    @Test
    void handlesSingleWord() {
        assertThat(tokenizer.tokenize("login")).containsExactly("login");
    }

    @Test
    void handlesMixedAbbreviations() {
        assertThat(tokenizer.tokenize("parseXMLDocument")).containsExactly("parse", "xml", "document");
    }

    @Test
    void normalizesToLowercase() {
        assertThat(tokenizer.tokenize("OrderService")).containsExactly("order", "service");
    }
}
