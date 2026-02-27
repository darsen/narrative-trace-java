package ai.narrativetrace.clarity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VerbDictionaryTest {

  private final VerbDictionary dictionary = new VerbDictionary();

  @Test
  void categorizeDomainSpecificVerb() {
    var result = dictionary.categorize("calculate");
    assertThat(result.category()).isEqualTo(VerbDictionary.Category.DOMAIN);
    assertThat(result.score()).isEqualTo(1.0);
  }

  @Test
  void categorizeStandardVerb() {
    var result = dictionary.categorize("create");
    assertThat(result.category()).isEqualTo(VerbDictionary.Category.STANDARD);
    assertThat(result.score()).isEqualTo(0.8);
  }

  @Test
  void categorizeGenericVerb() {
    var result = dictionary.categorize("process");
    assertThat(result.category()).isEqualTo(VerbDictionary.Category.GENERIC);
    assertThat(result.score()).isEqualTo(0.4);
  }

  @Test
  void categorizeBooleanPrefix() {
    var result = dictionary.categorize("is");
    assertThat(result.category()).isEqualTo(VerbDictionary.Category.BOOLEAN_PREFIX);
    assertThat(result.score()).isEqualTo(1.0);
  }

  @Test
  void categorizesNewDomainVerb() {
    var result = dictionary.categorize("order");
    assertThat(result.category()).isEqualTo(VerbDictionary.Category.DOMAIN);
    assertThat(result.score()).isEqualTo(1.0);
  }

  @Test
  void categorizesFinanceDomainVerb() {
    var result = dictionary.categorize("disburse");
    assertThat(result.category()).isEqualTo(VerbDictionary.Category.DOMAIN);
    assertThat(result.score()).isEqualTo(1.0);
  }

  @Test
  void categorizesGamingDomainVerb() {
    var result = dictionary.categorize("spawn");
    assertThat(result.category()).isEqualTo(VerbDictionary.Category.DOMAIN);
    assertThat(result.score()).isEqualTo(1.0);
  }

  @Test
  void returnsUnknownForUnrecognizedWord() {
    var result = dictionary.categorize("quux");
    assertThat(result.category()).isEqualTo(VerbDictionary.Category.UNKNOWN);
  }

  @Test
  void isCaseInsensitive() {
    var result = dictionary.categorize("Calculate");
    assertThat(result.category()).isEqualTo(VerbDictionary.Category.DOMAIN);
    assertThat(result.score()).isEqualTo(1.0);
  }
}
