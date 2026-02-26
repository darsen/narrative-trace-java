plugins {
    java
    kotlin("jvm") version "2.1.10"
}

kotlin {
    compilerOptions {
        javaParameters.set(true)
    }
}

dependencies {
    implementation(project(":narrativetrace-core"))
    implementation(project(":narrativetrace-proxy"))
    implementation(project(":narrativetrace-junit5"))
    implementation(project(":narrativetrace-diagrams"))
    implementation(project(":narrativetrace-slf4j"))
    implementation(project(":narrativetrace-spring"))
    implementation(project(":narrativetrace-micrometer"))
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("org.springframework:spring-context:6.2.3")
    implementation("io.micrometer:context-propagation:1.1.2")
    implementation("net.sourceforge.plantuml:plantuml-lgpl:1.2024.8")
    runtimeOnly("ch.qos.logback:logback-classic:1.5.15")

    implementation(kotlin("stdlib"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.assertj:assertj-core:3.27.3")
}

tasks.register<JavaExec>("runEcommerce") {
    description = "Run the e-commerce example"
    mainClass.set("ai.narrativetrace.examples.ecommerce.ECommerceExample")
    classpath = sourceSets["main"].runtimeClasspath
    jvmArgs = listOf("-Dlogback.configurationFile=logback-ecommerce.xml")
}

tasks.register<JavaExec>("runMinecraft") {
    description = "Run the Minecraft naming example"
    mainClass.set("ai.narrativetrace.examples.minecraft.MinecraftExample")
    classpath = sourceSets["main"].runtimeClasspath
    jvmArgs = listOf("-Dlogback.configurationFile=logback-minecraft.xml")
}

tasks.register<JavaExec>("runLibrary") {
    description = "Run the Kotlin library example"
    mainClass.set("ai.narrativetrace.examples.library.LibraryExample")
    classpath = sourceSets["main"].runtimeClasspath
    jvmArgs = listOf("-Dlogback.configurationFile=logback-library.xml")
}

tasks.register<JavaExec>("runClarity") {
    description = "Run the clarity demo example (mixed naming quality)"
    mainClass.set("ai.narrativetrace.examples.clarity.ClarityDemoExample")
    classpath = sourceSets["main"].runtimeClasspath
    jvmArgs = listOf("-Dlogback.configurationFile=logback-clarity.xml")
}

tasks.register("run") {
    description = "Run all examples"
    dependsOn("runEcommerce", "runMinecraft", "runLibrary", "runClarity")
}

fun Test.configureTraceOutput(format: String) {
    useJUnitPlatform()
    val traceDir = layout.buildDirectory.dir("narrativetrace").get().asFile
    // narrativetrace.output=true comes from junit-platform.properties
    systemProperty("narrativetrace.outputDir", traceDir.absolutePath)
    systemProperty("narrativetrace.format", format)
    exclude("**/MarkdownDocumentTest.class")
    exclude("**/unrefactored/**")
    testLogging {
        showStandardStreams = true
    }
    doFirst {
        if (traceDir.exists()) {
            traceDir.deleteRecursively()
        }
    }
    doLast {
        if (traceDir.exists()) {
            val files = traceDir.walkTopDown().filter { it.isFile }.toList()
            if (files.isNotEmpty()) {
                println("\n--- Trace files written (${files.size}) ---")
                files.forEach { println("  ${it.absolutePath}") }
                println("---")
            }
        }
    }
}

tasks.register<Test>("traceTests") {
    description = "Run tests and write Markdown trace files to build/narrativetrace/"
    configureTraceOutput("markdown")
}

tasks.register<Test>("traceTexts") {
    description = "Run tests and write indented text trace files to build/narrativetrace/"
    configureTraceOutput("text")
}

tasks.register<Test>("traceMermaid") {
    description = "Run tests and write Mermaid sequence diagram files to build/narrativetrace/"
    configureTraceOutput("mermaid")
}

tasks.register<Test>("tracePlantUml") {
    description = "Run tests and write PlantUML sequence diagram files to build/narrativetrace/"
    configureTraceOutput("plantuml")
}

tasks.register<JavaExec>("renderDiagrams") {
    description = "Run tracePlantUml then render .puml files to .svg images"
    dependsOn("tracePlantUml")
    mainClass.set("ai.narrativetrace.examples.PlantUmlImageRenderer")
    classpath = sourceSets["main"].runtimeClasspath
    args = listOf(layout.buildDirectory.dir("narrativetrace").get().asFile.absolutePath)
}
