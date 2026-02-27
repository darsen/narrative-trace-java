plugins {
    id("narrativetrace-publish")
}

extra["publishName"] = "NarrativeTrace Agent"
extra["publishDescription"] = "Java bytecode agent for automatic method tracing"

dependencies {
    implementation(project(":narrativetrace-core"))
    implementation("org.ow2.asm:asm:9.7.1")
    implementation("org.ow2.asm:asm-commons:9.7.1")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.assertj:assertj-core:3.27.3")
}

tasks.jar {
    manifest {
        attributes(
            "Premain-Class" to "ai.narrativetrace.agent.NarrativeTraceAgent",
            "Can-Retransform-Classes" to "true"
        )
    }
}
