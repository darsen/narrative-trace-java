dependencies {
    api(project(":narrativetrace-core"))
    implementation("io.micrometer:context-propagation:1.1.2")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.assertj:assertj-core:3.27.3")
}
