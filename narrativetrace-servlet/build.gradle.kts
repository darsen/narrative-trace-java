plugins {
    id("narrativetrace-publish")
}

extra["publishName"] = "NarrativeTrace Servlet"
extra["publishDescription"] = "Servlet filter for request lifecycle tracing"

dependencies {
    implementation(project(":narrativetrace-core"))
    compileOnly("jakarta.servlet:jakarta.servlet-api:6.0.0")
    implementation("org.slf4j:slf4j-api:2.0.16")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("jakarta.servlet:jakarta.servlet-api:6.0.0")
    testImplementation("ch.qos.logback:logback-classic:1.5.15")
}
