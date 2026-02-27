plugins {
    id("narrativetrace-publish")
}

extra["publishName"] = "NarrativeTrace Diagrams"
extra["publishDescription"] = "Mermaid and PlantUML sequence diagram generation"

dependencies {
    implementation(project(":narrativetrace-core"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.assertj:assertj-core:3.27.3")
}
