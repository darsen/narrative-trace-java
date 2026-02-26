package ai.narrativetrace.clarity;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class CollocationDictionary {

    // --- Finance & Banking ---
    private static final Map<String, Set<String>> FINANCE = Map.ofEntries(
            Map.entry("account", Set.of("debit", "credit", "balance", "close", "reconcile", "freeze")),
            Map.entry("ledger", Set.of("reconcile", "balance", "post", "close")),
            Map.entry("payment", Set.of("authorize", "capture", "disburse", "remit", "settle", "refund", "void")),
            Map.entry("loan", Set.of("originate", "underwrite", "amortize", "service", "default")),
            Map.entry("invoice", Set.of("issue", "settle", "void", "dispute")),
            Map.entry("transaction", Set.of("commit", "rollback", "authorize", "settle", "void", "reverse")),
            Map.entry("portfolio", Set.of("rebalance", "diversify", "hedge", "liquidate")),
            Map.entry("bond", Set.of("issue", "mature", "redeem", "yield", "coupon")),
            Map.entry("tax", Set.of("withhold", "file", "remit", "assess", "levy", "exempt")),
            Map.entry("budget", Set.of("allocate", "forecast", "reconcile", "approve")),
            Map.entry("asset", Set.of("value", "revalue", "impair", "liquidate")),
            Map.entry("collateral", Set.of("pledge", "release", "haircut"))
    );

    // --- E-Commerce & Retail ---
    private static final Map<String, Set<String>> ECOMMERCE = Map.ofEntries(
            Map.entry("order", Set.of("place", "fulfill", "cancel", "ship", "return", "backorder")),
            Map.entry("cart", Set.of("add", "remove", "empty", "checkout", "abandon")),
            Map.entry("inventory", Set.of("replenish", "reserve", "deplete", "count", "restock")),
            Map.entry("product", Set.of("list", "delist", "discount", "bundle", "feature")),
            Map.entry("subscription", Set.of("activate", "cancel", "renew", "pause", "upgrade", "downgrade")),
            Map.entry("coupon", Set.of("apply", "redeem", "expire", "validate")),
            Map.entry("price", Set.of("set", "reprice", "discount", "markdown")),
            Map.entry("return", Set.of("authorize", "receive", "refund", "restock"))
    );

    // --- Healthcare & Medical ---
    private static final Map<String, Set<String>> HEALTHCARE = Map.ofEntries(
            Map.entry("patient", Set.of("admit", "discharge", "refer", "triage", "diagnose", "treat")),
            Map.entry("medication", Set.of("prescribe", "administer", "dispense", "discontinue", "titrate")),
            Map.entry("appointment", Set.of("schedule", "cancel", "reschedule", "confirm", "checkin")),
            Map.entry("diagnosis", Set.of("confirm", "rule", "differential", "code")),
            Map.entry("record", Set.of("chart", "amend", "seal", "release")),
            Map.entry("vaccine", Set.of("administer", "store", "discard")),
            Map.entry("specimen", Set.of("collect", "label", "process"))
    );

    // --- Hospitality & Travel ---
    private static final Map<String, Set<String>> HOSPITALITY = Map.ofEntries(
            Map.entry("reservation", Set.of("book", "confirm", "cancel", "modify", "honor", "overbook")),
            Map.entry("room", Set.of("assign", "vacate", "upgrade", "block", "service")),
            Map.entry("guest", Set.of("checkin", "checkout", "accommodate", "bill", "comp")),
            Map.entry("booking", Set.of("rebook", "confirm", "cancel")),
            Map.entry("seat", Set.of("assign", "upgrade", "downgrade"))
    );

    // --- Telecommunications ---
    private static final Map<String, Set<String>> TELECOM = Map.ofEntries(
            Map.entry("call", Set.of("route", "drop", "forward", "transfer", "mute", "hold", "record")),
            Map.entry("signal", Set.of("amplify", "attenuate", "modulate", "demodulate", "broadcast")),
            Map.entry("channel", Set.of("allocate", "multiplex", "tune", "scramble")),
            Map.entry("subscriber", Set.of("provision", "suspend", "activate", "port", "throttle")),
            Map.entry("session", Set.of("originate", "terminate", "handoff")),
            Map.entry("bandwidth", Set.of("allocate", "shape", "throttle"))
    );

    // --- Gaming ---
    private static final Map<String, Set<String>> GAMING = Map.ofEntries(
            Map.entry("player", Set.of("spawn", "respawn", "ban", "kick", "matchmake", "rank")),
            Map.entry("item", Set.of("equip", "loot", "craft", "enchant", "disenchant", "trade")),
            Map.entry("character", Set.of("level", "buff", "debuff", "heal", "revive", "nerf")),
            Map.entry("match", Set.of("start", "pause", "forfeit", "abandon", "spectate")),
            Map.entry("queue", Set.of("join", "leave", "matchmake")),
            Map.entry("lobby", Set.of("create", "join", "leave"))
    );

    // --- Logistics & Supply Chain ---
    private static final Map<String, Set<String>> LOGISTICS = Map.ofEntries(
            Map.entry("shipment", Set.of("dispatch", "track", "reroute", "deliver", "return", "insure")),
            Map.entry("cargo", Set.of("load", "unload", "stow", "manifest", "inspect", "clear")),
            Map.entry("route", Set.of("plan", "optimize", "divert", "schedule")),
            Map.entry("container", Set.of("load", "seal", "unseal", "transload")),
            Map.entry("dock", Set.of("assign", "slot", "release"))
    );

    // --- Insurance ---
    private static final Map<String, Set<String>> INSURANCE = Map.ofEntries(
            Map.entry("policy", Set.of("underwrite", "issue", "renew", "cancel", "lapse", "reinstate", "endorse")),
            Map.entry("claim", Set.of("file", "adjust", "settle", "deny", "subrogate", "appeal")),
            Map.entry("premium", Set.of("quote", "calculate", "collect", "waive", "refund")),
            Map.entry("endorsement", Set.of("add", "remove", "amend")),
            Map.entry("deductible", Set.of("apply", "waive"))
    );

    // --- Education ---
    private static final Map<String, Set<String>> EDUCATION = Map.ofEntries(
            Map.entry("student", Set.of("enroll", "expel", "graduate", "mentor", "counsel", "assess")),
            Map.entry("course", Set.of("register", "audit", "drop", "complete", "accredit")),
            Map.entry("grade", Set.of("assign", "appeal", "curve", "post", "withhold")),
            Map.entry("attendance", Set.of("record", "audit", "verify")),
            Map.entry("exam", Set.of("schedule", "proctor", "grade"))
    );

    // --- Real Estate & Property ---
    private static final Map<String, Set<String>> REAL_ESTATE = Map.ofEntries(
            Map.entry("property", Set.of("list", "appraise", "inspect", "close", "escrow", "foreclose")),
            Map.entry("lease", Set.of("sign", "renew", "terminate", "sublease", "amend")),
            Map.entry("tenant", Set.of("screen", "evict", "accommodate", "bill")),
            Map.entry("title", Set.of("search", "clear", "record")),
            Map.entry("showing", Set.of("schedule", "cancel"))
    );

    // --- HR & Workforce ---
    private static final Map<String, Set<String>> HR = Map.ofEntries(
            Map.entry("employee", Set.of("hire", "onboard", "promote", "demote", "terminate", "furlough", "transfer")),
            Map.entry("candidate", Set.of("screen", "interview", "recruit", "reject", "shortlist")),
            Map.entry("position", Set.of("post", "fill", "eliminate", "reclassify")),
            Map.entry("headcount", Set.of("plan", "reduce", "increase")),
            Map.entry("compensation", Set.of("benchmark", "adjust", "approve"))
    );

    // --- Security & Authentication ---
    private static final Map<String, Set<String>> SECURITY = Map.ofEntries(
            Map.entry("token", Set.of("issue", "revoke", "refresh", "rotate", "invalidate", "blacklist")),
            Map.entry("credential", Set.of("verify", "revoke", "hash", "store", "rotate")),
            Map.entry("session", Set.of("create", "invalidate", "extend", "hijack", "terminate")),
            Map.entry("certificate", Set.of("sign", "revoke", "renew", "chain", "pin")),
            Map.entry("key", Set.of("generate", "rotate", "revoke", "archive")),
            Map.entry("acl", Set.of("enforce", "evaluate", "audit"))
    );

    // --- DevOps & Infrastructure ---
    private static final Map<String, Set<String>> DEVOPS = Map.ofEntries(
            Map.entry("instance", Set.of("provision", "deploy", "scale", "terminate", "snapshot", "migrate")),
            Map.entry("container", Set.of("build", "deploy", "kill", "restart", "orchestrate")),
            Map.entry("pipeline", Set.of("trigger", "run", "abort", "retry", "promote")),
            Map.entry("cache", Set.of("warm", "invalidate", "evict", "flush", "populate")),
            Map.entry("node", Set.of("cordon", "drain", "uncordon")),
            Map.entry("release", Set.of("promote", "rollback", "rollout"))
    );

    // --- Data & Analytics ---
    private static final Map<String, Set<String>> DATA = Map.ofEntries(
            Map.entry("dataset", Set.of("ingest", "cleanse", "partition", "sample", "anonymize")),
            Map.entry("schema", Set.of("migrate", "validate", "version", "evolve", "normalize")),
            Map.entry("index", Set.of("build", "rebuild", "drop", "optimize", "shard")),
            Map.entry("query", Set.of("execute", "optimize", "cache", "paginate", "throttle")),
            Map.entry("feature", Set.of("derive", "normalize", "vectorize")),
            Map.entry("window", Set.of("slide", "aggregate", "rank"))
    );

    // --- Content & Media ---
    private static final Map<String, Set<String>> CONTENT = Map.ofEntries(
            Map.entry("article", Set.of("draft", "publish", "archive", "retract", "syndicate")),
            Map.entry("comment", Set.of("post", "moderate", "flag", "delete", "pin")),
            Map.entry("media", Set.of("upload", "transcode", "stream", "caption", "watermark")),
            Map.entry("subtitle", Set.of("generate", "sync", "translate")),
            Map.entry("transcript", Set.of("generate", "edit", "publish"))
    );

    // --- Social & Community ---
    private static final Map<String, Set<String>> SOCIAL = Map.ofEntries(
            Map.entry("user", Set.of("follow", "unfollow", "block", "mute", "report", "verify")),
            Map.entry("post", Set.of("publish", "pin", "boost", "archive", "flag")),
            Map.entry("thread", Set.of("start", "lock", "archive")),
            Map.entry("message", Set.of("send", "delete", "unsend"))
    );

    // --- Messaging & Events ---
    private static final Map<String, Set<String>> MESSAGING = Map.ofEntries(
            Map.entry("message", Set.of("enqueue", "dequeue", "acknowledge", "nack", "retry", "deadletter")),
            Map.entry("event", Set.of("emit", "publish", "replay", "fanout", "route")),
            Map.entry("queue", Set.of("drain", "purge", "park", "resume"))
    );

    // --- IoT & Embedded ---
    private static final Map<String, Set<String>> IOT = Map.ofEntries(
            Map.entry("device", Set.of("provision", "commission", "decommission", "pair", "unpair", "reboot")),
            Map.entry("sensor", Set.of("calibrate", "sample", "poll", "stream")),
            Map.entry("telemetry", Set.of("capture", "ingest", "aggregate"))
    );

    // --- Legal & Compliance ---
    private static final Map<String, Set<String>> LEGAL = Map.ofEntries(
            Map.entry("contract", Set.of("draft", "sign", "amend", "terminate", "enforce", "breach")),
            Map.entry("case", Set.of("file", "adjudicate", "dismiss", "settle", "appeal")),
            Map.entry("verdict", Set.of("deliver", "appeal", "overturn", "uphold")),
            Map.entry("motion", Set.of("file", "argue", "grant", "deny")),
            Map.entry("brief", Set.of("draft", "file", "amend"))
    );

    // --- Manufacturing ---
    private static final Map<String, Set<String>> MANUFACTURING = Map.ofEntries(
            Map.entry("batch", Set.of("start", "inspect", "reject", "release", "quarantine")),
            Map.entry("component", Set.of("assemble", "solder", "weld", "test", "certify")),
            Map.entry("line", Set.of("start", "stop", "balance", "retool")),
            Map.entry("workorder", Set.of("create", "schedule", "close"))
    );

    // --- Agriculture & Food ---
    private static final Map<String, Set<String>> AGRICULTURE = Map.ofEntries(
            Map.entry("field", Set.of("plant", "irrigate", "fertilize", "harvest")),
            Map.entry("crop", Set.of("sow", "spray", "prune", "harvest")),
            Map.entry("livestock", Set.of("feed", "breed", "vaccinate", "wean"))
    );

    // --- Advertising & Marketing ---
    private static final Map<String, Set<String>> ADVERTISING = Map.ofEntries(
            Map.entry("campaign", Set.of("launch", "pause", "optimize", "target", "remarket")),
            Map.entry("audience", Set.of("segment", "target", "exclude", "expand")),
            Map.entry("creative", Set.of("draft", "review", "approve", "rotate"))
    );

    // --- Transportation ---
    private static final Map<String, Set<String>> TRANSPORTATION = Map.ofEntries(
            Map.entry("flight", Set.of("schedule", "delay", "depart", "arrive", "reroute")),
            Map.entry("vessel", Set.of("berth", "moor", "unmoor", "dock")),
            Map.entry("vehicle", Set.of("dispatch", "refuel", "reroute", "park"))
    );

    // --- Energy & Utilities ---
    private static final Map<String, Set<String>> ENERGY = Map.ofEntries(
            Map.entry("grid", Set.of("balance", "stabilize", "shed", "curtail", "interconnect")),
            Map.entry("meter", Set.of("read", "calibrate", "install", "replace", "tamper")),
            Map.entry("feeder", Set.of("energize", "deenergize", "switch", "island")),
            Map.entry("plant", Set.of("dispatch", "ramp", "derate", "blackstart"))
    );

    // --- Blockchain & Crypto ---
    private static final Map<String, Set<String>> BLOCKCHAIN = Map.ofEntries(
            Map.entry("token", Set.of("mint", "burn", "stake", "transfer", "vest", "lock")),
            Map.entry("contract", Set.of("deploy", "verify", "audit", "upgrade", "pause")),
            Map.entry("validator", Set.of("delegate", "redelegate", "slash", "unbond")),
            Map.entry("bridge", Set.of("lock", "mint", "burn", "release"))
    );

    // --- Government & Public Sector ---
    private static final Map<String, Set<String>> PUBLIC_SECTOR = Map.ofEntries(
            Map.entry("permit", Set.of("issue", "renew", "revoke", "approve")),
            Map.entry("license", Set.of("issue", "renew", "suspend", "revoke")),
            Map.entry("ordinance", Set.of("draft", "enact", "amend", "repeal"))
    );

    // --- Pharmaceuticals & Biotechnology ---
    private static final Map<String, Set<String>> PHARMA_BIOTECH = Map.ofEntries(
            Map.entry("assay", Set.of("run", "validate", "repeat")),
            Map.entry("sample", Set.of("aliquot", "dilute", "incubate", "analyze")),
            Map.entry("compound", Set.of("synthesize", "formulate", "stabilize"))
    );

    // --- Customer Support & CRM ---
    private static final Map<String, Set<String>> SUPPORT_CRM = Map.ofEntries(
            Map.entry("ticket", Set.of("open", "triage", "assign", "escalate", "resolve", "close")),
            Map.entry("case", Set.of("categorize", "prioritize", "reopen", "resolve")),
            Map.entry("customer", Set.of("notify", "update", "verify", "retain"))
    );

    // --- Payments & Fintech ---
    private static final Map<String, Set<String>> PAYMENTS_FINTECH = Map.ofEntries(
            Map.entry("payout", Set.of("initiate", "disburse", "settle", "reverse")),
            Map.entry("chargeback", Set.of("file", "dispute", "win", "lose")),
            Map.entry("authorization", Set.of("request", "reauthorize", "decline", "approve"))
    );

    // --- Media AdTech ---
    private static final Map<String, Set<String>> MEDIA_ADTECH = Map.ofEntries(
            Map.entry("impression", Set.of("serve", "count", "cap", "pace")),
            Map.entry("bid", Set.of("submit", "win", "lose", "optimize")),
            Map.entry("audience", Set.of("segment", "target", "expand", "suppress"))
    );

    private static final Map<String, Set<String>> ALL_COLLOCATIONS = Stream.of(
            FINANCE, ECOMMERCE, HEALTHCARE, HOSPITALITY, TELECOM, GAMING,
            LOGISTICS, INSURANCE, EDUCATION, REAL_ESTATE, HR, SECURITY,
            DEVOPS, DATA, CONTENT, SOCIAL, MESSAGING, IOT, LEGAL, MANUFACTURING,
            AGRICULTURE, ADVERTISING, TRANSPORTATION, ENERGY, BLOCKCHAIN,
            PUBLIC_SECTOR, PHARMA_BIOTECH, SUPPORT_CRM, PAYMENTS_FINTECH, MEDIA_ADTECH)
            .flatMap(m -> m.entrySet().stream())
            .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> new HashSet<>(e.getValue()),
                    (a, b) -> { a.addAll(b); return a; }))
            .entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> Set.copyOf(e.getValue())));

    public Set<String> preferredVerbs(String noun) {
        if (noun == null || noun.isBlank()) return Set.of();
        return ALL_COLLOCATIONS.getOrDefault(noun.toLowerCase(Locale.ROOT), Set.of());
    }

    public boolean isPreferred(String verb, String noun) {
        if (verb == null || verb.isBlank()) return false;
        return preferredVerbs(noun).contains(verb.toLowerCase(Locale.ROOT));
    }
}
