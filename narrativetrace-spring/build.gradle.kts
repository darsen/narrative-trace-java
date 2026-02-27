plugins {
    id("narrativetrace-publish")
}

extra["publishName"] = "NarrativeTrace Spring"
extra["publishDescription"] = "Spring Framework integration with BeanPostProcessor"

dependencies {
    implementation(project(":narrativetrace-core"))
    implementation(project(":narrativetrace-proxy"))
    implementation("org.springframework:spring-context:6.2.3")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("org.springframework:spring-test:6.2.3")
}
