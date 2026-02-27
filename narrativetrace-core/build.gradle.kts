plugins {
    id("narrativetrace-publish")
}

extra["publishName"] = "NarrativeTrace Core"
extra["publishDescription"] = "Auto-generates human-readable execution traces. Zero runtime dependencies."

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("net.jqwik:jqwik:1.9.2")
}
