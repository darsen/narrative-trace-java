plugins {
    id("narrativetrace-publish")
}

extra["publishName"] = "NarrativeTrace Micrometer"
extra["publishDescription"] = "Micrometer context-propagation bridge for cross-thread tracing"

dependencies {
    api(project(":narrativetrace-core"))
    implementation("io.micrometer:context-propagation:1.1.2")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.assertj:assertj-core:3.27.3")
}
