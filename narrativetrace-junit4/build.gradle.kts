tasks.test {
    exclude("**/Junit4FailingFixture.class")
    exclude("**/Junit4MultiTestFixture.class")
    exclude("**/Junit4EmptyTraceFixture.class")

}

dependencies {
    api(project(":narrativetrace-core"))
    api(project(":narrativetrace-proxy"))
    api(project(":narrativetrace-diagrams"))
    api(project(":narrativetrace-clarity"))
    api("junit:junit:4.13.2")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.junit.vintage:junit-vintage-engine:5.11.4")
    testImplementation("org.junit.platform:junit-platform-launcher:1.11.4")
    testImplementation("org.assertj:assertj-core:3.27.3")
}
