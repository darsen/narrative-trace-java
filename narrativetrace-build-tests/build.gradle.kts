dependencies {
    testImplementation(gradleTestKit())
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.assertj:assertj-core:3.27.3")
}

tasks.withType<Test> {
    systemProperty("projectDir", rootProject.projectDir.absolutePath)
}
