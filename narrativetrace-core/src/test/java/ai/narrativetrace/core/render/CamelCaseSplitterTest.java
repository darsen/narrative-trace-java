package ai.narrativetrace.core.render;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CamelCaseSplitterTest {

    @Test
    void split_singleWord() {
        assertThat(CamelCaseSplitter.split("order")).containsExactly("order");
    }

    @Test
    void split_camelCase() {
        assertThat(CamelCaseSplitter.split("calculateTotal")).containsExactly("calculate", "Total");
    }

    @Test
    void toPhrase_camelCase() {
        assertThat(CamelCaseSplitter.toPhrase("calculateTotal")).isEqualTo("calculate total");
    }
}
