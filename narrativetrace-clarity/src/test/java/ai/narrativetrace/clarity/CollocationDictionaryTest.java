package ai.narrativetrace.clarity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CollocationDictionaryTest {

  private final CollocationDictionary dictionary = new CollocationDictionary();

  @Test
  void returnsPreferredVerbsForFinanceNoun() {
    var verbs = dictionary.preferredVerbs("ledger");
    assertThat(verbs).containsExactlyInAnyOrder("reconcile", "balance", "post", "close");
  }

  @Test
  void returnsEmptySetForUnknownNoun() {
    assertThat(dictionary.preferredVerbs("quuxbaz")).isEmpty();
  }

  @Test
  void isPreferredReturnsTrueForKnownCollocation() {
    assertThat(dictionary.isPreferred("reconcile", "ledger")).isTrue();
  }

  @Test
  void isPreferredReturnsFalseForNonPreferredVerb() {
    assertThat(dictionary.isPreferred("check", "ledger")).isFalse();
  }

  @Test
  void isCaseInsensitive() {
    assertThat(dictionary.preferredVerbs("Ledger"))
        .containsExactlyInAnyOrder("reconcile", "balance", "post", "close");
    assertThat(dictionary.isPreferred("Reconcile", "LEDGER")).isTrue();
  }

  @Test
  void financeAccountHasExpectedVerbs() {
    var verbs = dictionary.preferredVerbs("account");
    assertThat(verbs)
        .containsExactlyInAnyOrder("debit", "credit", "balance", "close", "reconcile", "freeze");
  }

  @Test
  void financePaymentHasExpectedVerbs() {
    var verbs = dictionary.preferredVerbs("payment");
    assertThat(verbs)
        .containsExactlyInAnyOrder(
            "authorize", "capture", "disburse", "remit", "settle", "refund", "void");
  }

  @Test
  void financeLoanHasExpectedVerbs() {
    var verbs = dictionary.preferredVerbs("loan");
    assertThat(verbs)
        .containsExactlyInAnyOrder("originate", "underwrite", "amortize", "service", "default");
  }

  @Test
  void ecommerceOrderHasExpectedVerbs() {
    var verbs = dictionary.preferredVerbs("order");
    assertThat(verbs)
        .containsExactlyInAnyOrder("place", "fulfill", "cancel", "ship", "return", "backorder");
  }

  @Test
  void ecommerceCartHasExpectedVerbs() {
    var verbs = dictionary.preferredVerbs("cart");
    assertThat(verbs).containsExactlyInAnyOrder("add", "remove", "empty", "checkout", "abandon");
  }

  @Test
  void ecommerceInventoryHasExpectedVerbs() {
    var verbs = dictionary.preferredVerbs("inventory");
    assertThat(verbs)
        .containsExactlyInAnyOrder("replenish", "reserve", "deplete", "count", "restock");
  }

  @Test
  void healthcarePatientHasExpectedVerbs() {
    var verbs = dictionary.preferredVerbs("patient");
    assertThat(verbs)
        .containsExactlyInAnyOrder("admit", "discharge", "refer", "triage", "diagnose", "treat");
  }

  @Test
  void healthcareMedicationHasExpectedVerbs() {
    var verbs = dictionary.preferredVerbs("medication");
    assertThat(verbs)
        .containsExactlyInAnyOrder("prescribe", "administer", "dispense", "discontinue", "titrate");
  }

  @Test
  void hospitalityReservationHasExpectedVerbs() {
    var verbs = dictionary.preferredVerbs("reservation");
    assertThat(verbs)
        .containsExactlyInAnyOrder("book", "confirm", "cancel", "modify", "honor", "overbook");
  }

  @Test
  void telecomCallHasExpectedVerbs() {
    var verbs = dictionary.preferredVerbs("call");
    assertThat(verbs)
        .containsExactlyInAnyOrder(
            "route", "drop", "forward", "transfer", "mute", "hold", "record");
  }

  @Test
  void gamingPlayerHasExpectedVerbs() {
    var verbs = dictionary.preferredVerbs("player");
    assertThat(verbs)
        .containsExactlyInAnyOrder("spawn", "respawn", "ban", "kick", "matchmake", "rank");
  }

  @Test
  void logisticsShipmentHasExpectedVerbs() {
    var verbs = dictionary.preferredVerbs("shipment");
    assertThat(verbs)
        .containsExactlyInAnyOrder("dispatch", "track", "reroute", "deliver", "return", "insure");
  }

  @Test
  void insurancePolicyHasExpectedVerbs() {
    var verbs = dictionary.preferredVerbs("policy");
    assertThat(verbs)
        .containsExactlyInAnyOrder(
            "underwrite", "issue", "renew", "cancel", "lapse", "reinstate", "endorse");
  }

  @Test
  void educationStudentHasExpectedVerbs() {
    var verbs = dictionary.preferredVerbs("student");
    assertThat(verbs)
        .containsExactlyInAnyOrder("enroll", "expel", "graduate", "mentor", "counsel", "assess");
  }

  @Test
  void realEstatePropertyHasExpectedVerbs() {
    var verbs = dictionary.preferredVerbs("property");
    assertThat(verbs)
        .containsExactlyInAnyOrder("list", "appraise", "inspect", "close", "escrow", "foreclose");
  }

  @Test
  void hrEmployeeHasExpectedVerbs() {
    var verbs = dictionary.preferredVerbs("employee");
    assertThat(verbs)
        .containsExactlyInAnyOrder(
            "hire", "onboard", "promote", "demote", "terminate", "furlough", "transfer");
  }

  @Test
  void securityAndBlockchainTokenVerbsAreMerged() {
    var verbs = dictionary.preferredVerbs("token");
    // Security: issue, revoke, refresh, rotate, invalidate, blacklist
    // Blockchain: mint, burn, stake, transfer, vest, lock
    assertThat(verbs)
        .containsExactlyInAnyOrder(
            "issue",
            "revoke",
            "refresh",
            "rotate",
            "invalidate",
            "blacklist",
            "mint",
            "burn",
            "stake",
            "transfer",
            "vest",
            "lock");
  }

  @Test
  void devopsInstanceHasExpectedVerbs() {
    var verbs = dictionary.preferredVerbs("instance");
    assertThat(verbs)
        .containsExactlyInAnyOrder(
            "provision", "deploy", "scale", "terminate", "snapshot", "migrate");
  }

  @Test
  void dataDatasetHasExpectedVerbs() {
    var verbs = dictionary.preferredVerbs("dataset");
    assertThat(verbs)
        .containsExactlyInAnyOrder("ingest", "cleanse", "partition", "sample", "anonymize");
  }

  @Test
  void contentArticleHasExpectedVerbs() {
    var verbs = dictionary.preferredVerbs("article");
    assertThat(verbs)
        .containsExactlyInAnyOrder("draft", "publish", "archive", "retract", "syndicate");
  }

  @Test
  void socialUserHasExpectedVerbs() {
    var verbs = dictionary.preferredVerbs("user");
    assertThat(verbs)
        .containsExactlyInAnyOrder("follow", "unfollow", "block", "mute", "report", "verify");
  }

  @Test
  void legalAndBlockchainContractVerbsAreMerged() {
    var verbs = dictionary.preferredVerbs("contract");
    // Legal: draft, sign, amend, terminate, enforce, breach
    // Blockchain: deploy, verify, audit, upgrade, pause
    assertThat(verbs)
        .containsExactlyInAnyOrder(
            "draft",
            "sign",
            "amend",
            "terminate",
            "enforce",
            "breach",
            "deploy",
            "verify",
            "audit",
            "upgrade",
            "pause");
  }

  @Test
  void manufacturingBatchHasExpectedVerbs() {
    var verbs = dictionary.preferredVerbs("batch");
    assertThat(verbs)
        .containsExactlyInAnyOrder("start", "inspect", "reject", "release", "quarantine");
  }

  @Test
  void energyGridHasExpectedVerbs() {
    var verbs = dictionary.preferredVerbs("grid");
    assertThat(verbs)
        .containsExactlyInAnyOrder("balance", "stabilize", "shed", "curtail", "interconnect");
  }
}
