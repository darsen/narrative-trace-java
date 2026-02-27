plugins {
    id("narrativetrace-publish")
}

extra["publishName"] = "NarrativeTrace SLF4J"
extra["publishDescription"] = "SLF4J bridge with MDC propagation"

dependencies {
    implementation(project(":narrativetrace-core"))
    implementation("org.slf4j:slf4j-api:2.0.16")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("ch.qos.logback:logback-classic:1.5.15")
}
