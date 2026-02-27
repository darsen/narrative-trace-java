package ai.narrativetrace.clarity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MethodNameScorerTest {

  private final MethodNameScorer scorer = new MethodNameScorer();

  @Test
  void scoresDomainVerbPlusNounHighly() {
    assertThat(scorer.score("calculateRefund")).isGreaterThanOrEqualTo(0.90);
    assertThat(scorer.score("validateCredentials")).isGreaterThanOrEqualTo(0.90);
    assertThat(scorer.score("reserveInventory")).isGreaterThanOrEqualTo(0.90);
  }

  @Test
  void scoresGenericSingleWordLow() {
    assertThat(scorer.score("process")).isLessThanOrEqualTo(0.15);
    assertThat(scorer.score("handle")).isLessThanOrEqualTo(0.15);
    assertThat(scorer.score("do")).isLessThanOrEqualTo(0.15);
    assertThat(scorer.score("run")).isLessThanOrEqualTo(0.15);
    assertThat(scorer.score("perform")).isLessThanOrEqualTo(0.15);
  }

  @Test
  void scoresGenericVerbPlusSpecificNounMedium() {
    double getCustomer = scorer.score("getCustomer");
    assertThat(getCustomer).isBetween(0.45, 0.65);

    double processOrder = scorer.score("processOrder");
    assertThat(processOrder).isBetween(0.45, 0.65);

    double handlePayment = scorer.score("handlePayment");
    assertThat(handlePayment).isBetween(0.45, 0.65);
  }

  @Test
  void scoresSingleNonGenericWordMedium() {
    double login = scorer.score("login");
    assertThat(login).isBetween(0.50, 0.70);

    double validate = scorer.score("validate");
    assertThat(validate).isBetween(0.50, 0.70);
  }

  @Test
  void penalizesAbbreviatedTokens() {
    double abbreviated = scorer.score("calcRefAmt");
    double full = scorer.score("calculateRefundAmount");
    assertThat(abbreviated).isLessThan(full);
  }

  @Test
  void penalizesExcessiveTokenCount() {
    double longName = scorer.score("calculateAndValidateAndProcessOrder");
    double shortName = scorer.score("calculateOrder");
    assertThat(longName).isLessThan(shortName);
  }

  @Test
  void rewardsMorphologicallyConfirmedVerb() {
    // "validate" has -ate suffix (morphologically confirmed verb)
    double validateOrder = scorer.score("validateOrder");
    // "fetch" is dictionary-only (standard verb, no morphological suffix)
    double fetchOrder = scorer.score("fetchOrder");
    assertThat(validateOrder).isGreaterThan(fetchOrder);
  }

  @Test
  void differentiatesPreviouslyEqualNames() {
    // These all scored 0.5 in the old system
    double getCustomer = scorer.score("getCustomer");
    double processOrder = scorer.score("processOrder");
    double handlePayment = scorer.score("handlePayment");

    // They should not ALL be exactly equal now
    boolean allEqual = getCustomer == processOrder && processOrder == handlePayment;
    assertThat(allEqual).isFalse();
  }

  @Test
  void scoresSingleBooleanPrefix() {
    double is = scorer.score("is");
    assertThat(is).isBetween(0.30, 0.50);
  }

  @Test
  void scoresSingleStandardVerb() {
    double create = scorer.score("create");
    assertThat(create).isBetween(0.35, 0.55);
  }

  @Test
  void scoresMorphologicalVerbWithoutDictionary() {
    // "customize" ends in -ize, morphologically a verb but not in domain verb list
    double customizeWidget = scorer.score("customizeWidget");
    assertThat(customizeWidget).isGreaterThan(0.50);
  }

  @Test
  void collocationDictionaryIsAccessible() {
    assertThat(scorer.collocationDictionary()).isNotNull();
    assertThat(scorer.collocationDictionary().isPreferred("reconcile", "ledger")).isTrue();
  }
}
