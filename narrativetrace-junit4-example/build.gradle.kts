dependencies {
    implementation(project(":narrativetrace-core"))
    implementation(project(":narrativetrace-proxy"))
    implementation(project(":narrativetrace-junit4"))

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.junit.vintage:junit-vintage-engine:5.11.4")
}
