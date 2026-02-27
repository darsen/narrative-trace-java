package ai.narrativetrace.clarity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RoleSuffixDictionaryTest {

  private final RoleSuffixDictionary dictionary = new RoleSuffixDictionary();

  @Test
  void classifiesDesignPatternSuffix() {
    var result = dictionary.classify("Service");
    assertThat(result.category()).isEqualTo(RoleSuffixDictionary.Category.DESIGN_PATTERN);
    assertThat(result.score()).isEqualTo(1.0);
  }

  @Test
  void classifiesFunctionalRole() {
    var result = dictionary.classify("Validator");
    assertThat(result.category()).isEqualTo(RoleSuffixDictionary.Category.FUNCTIONAL);
    assertThat(result.score()).isEqualTo(1.0);
  }

  @Test
  void classifiesGenericRole() {
    var result = dictionary.classify("Manager");
    assertThat(result.category()).isEqualTo(RoleSuffixDictionary.Category.GENERIC);
    assertThat(result.score()).isEqualTo(0.3);
  }

  @Test
  void returnsUnknownForNonRole() {
    var result = dictionary.classify("Order");
    assertThat(result.category()).isEqualTo(RoleSuffixDictionary.Category.UNKNOWN);
  }

  @Test
  void returnsExpectedVerbsForRepository() {
    var verbs = dictionary.expectedVerbs("Repository");
    assertThat(verbs).contains("find", "save", "delete");
  }

  @Test
  void isCaseInsensitive() {
    var result = dictionary.classify("service");
    assertThat(result.category()).isEqualTo(RoleSuffixDictionary.Category.DESIGN_PATTERN);
    assertThat(result.score()).isEqualTo(1.0);
  }
}
