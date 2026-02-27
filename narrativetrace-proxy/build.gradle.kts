plugins {
    id("narrativetrace-publish")
}

extra["publishName"] = "NarrativeTrace Proxy"
extra["publishDescription"] = "JDK dynamic proxy for automatic method tracing"

dependencies {
    api(project(":narrativetrace-core"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("net.jqwik:jqwik:1.9.2")
}
