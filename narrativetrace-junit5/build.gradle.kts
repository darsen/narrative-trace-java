plugins {
    id("narrativetrace-publish")
}

extra["publishName"] = "NarrativeTrace JUnit 5"
extra["publishDescription"] = "JUnit 5 extension for automatic trace output"

tasks.test {
    exclude("**/FailingTestFixture.class")
}

dependencies {
    api(project(":narrativetrace-core"))
    api(project(":narrativetrace-proxy"))
    api(project(":narrativetrace-diagrams"))
    api(project(":narrativetrace-clarity"))
    api("org.junit.jupiter:junit-jupiter-api:5.11.4")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.junit.platform:junit-platform-launcher:1.11.4")
    testImplementation("org.assertj:assertj-core:3.27.3")
}
