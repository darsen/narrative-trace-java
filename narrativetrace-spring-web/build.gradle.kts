plugins {
    id("narrativetrace-publish")
}

extra["publishName"] = "NarrativeTrace Spring Web"
extra["publishDescription"] = "Spring Web integration for servlet filter configuration"

dependencies {
    implementation(project(":narrativetrace-core"))
    implementation(project(":narrativetrace-servlet"))
    implementation(project(":narrativetrace-spring"))
    implementation("org.springframework:spring-context:6.2.3")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("org.springframework:spring-test:6.2.3")
    testImplementation("org.springframework:spring-web:6.2.3")
    testImplementation("jakarta.servlet:jakarta.servlet-api:6.0.0")
    testImplementation("ch.qos.logback:logback-classic:1.5.15")
}
