repositories {
    mavenCentral()
}

val publishedModules = subprojects.filter { sub ->
    sub.name !in setOf(
        "narrativetrace-benchmarks",
        "narrativetrace-build-tests",
        "narrativetrace-gradle-plugin",
        "narrativetrace-examples",
        "narrativetrace-junit4-example"
    )
}

tasks.register<Javadoc>("aggregateJavadoc") {
    description = "Generates aggregated Javadoc across all published modules"
    group = "documentation"
    title = "NarrativeTrace API"
    dependsOn(publishedModules.map { it.tasks.named("classes") })
    source(publishedModules.map { it.the<SourceSetContainer>()["main"].allJava })
    classpath = files(publishedModules.map { it.the<SourceSetContainer>()["main"].compileClasspath })
    destinationDir = layout.buildDirectory.dir("docs/aggregateJavadoc").get().asFile
    options {
        this as StandardJavadocDocletOptions
        addStringOption("Xdoclint:none", "-quiet")
        links("https://docs.oracle.com/en/java/javase/17/docs/api/")
    }
}

tasks.register<Copy>("generateLlmsDocs") {
    description = "Copies llms-full.md to build/site/llms-full.txt and generates llms.txt"
    group = "documentation"
    from("documentation/llms-full.md")
    into(layout.buildDirectory.dir("site"))
    rename("llms-full.md", "llms-full.txt")
    doLast {
        val llmsTxt = file("documentation/llms.txt")
        if (llmsTxt.exists()) {
            llmsTxt.copyTo(layout.buildDirectory.file("site/llms.txt").get().asFile, overwrite = true)
        }
    }
}

tasks.register("pitest") {
    description = "Runs mutation testing (PIT) across core, proxy, and clarity modules"
    group = "verification"
    dependsOn(":narrativetrace-core:pitest", ":narrativetrace-proxy:pitest", ":narrativetrace-clarity:pitest")
}

tasks.register("metricsReport") {
    description = "Aggregates NCSS metrics from all modules, sorted by size (descending)"
    group = "verification"
    dependsOn(subprojects.map { it.tasks.named("pmdMetrics") })
    doLast {
        val entries = ai.narrativetrace.build.MetricsReportSupport.collectEntriesFromProjects(subprojects)
        ai.narrativetrace.build.MetricsReportSupport.printReport(entries)
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "jacoco")
    apply(plugin = "pmd")
    apply(plugin = "com.diffplug.spotless")

    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        java {
            googleJavaFormat("1.25.2")
            targetExclude("build/**")
        }
        if (project.name == "narrativetrace-examples") {
            kotlin {
                ktlint("1.5.0")
                targetExclude("build/**")
            }
        }
    }

    group = "ai.narrativetrace"
    version = "0.2.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    tasks.withType<JavaCompile> {
        options.compilerArgs.add("-parameters")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        System.getProperty("narrativetrace.output")?.let { systemProperty("narrativetrace.output", it) }
        System.getProperty("narrativetrace.outputDir")?.let { systemProperty("narrativetrace.outputDir", it) }
        System.getProperty("narrativetrace.format")?.let { systemProperty("narrativetrace.format", it) }
        finalizedBy(tasks.named("jacocoTestReport"))
    }

    tasks.register("printCompilerArgs") {
        doLast {
            tasks.withType<JavaCompile>().forEach {
                println("COMPILER_ARGS: ${it.options.compilerArgs}")
            }
        }
    }

    configure<PmdExtension> {
        toolVersion = "7.8.0"
        isConsoleOutput = true
        isIgnoreFailures = false
        ruleSets = emptyList()
        ruleSetFiles = rootProject.files("config/pmd/ruleset.xml")
    }

    val java = the<JavaPluginExtension>()
    tasks.register<Pmd>("pmdMetrics") {
        description = "Reports NCSS (non-commenting source statements) per method and class"
        group = "verification"
        source = java.sourceSets["main"].allJava
        classpath = java.sourceSets["main"].compileClasspath
        ruleSets = emptyList()
        ruleSetFiles = rootProject.files("config/pmd/metrics.xml")
        isConsoleOutput = false
        setIgnoreFailures(true)
        reports {
            xml.required.set(true)
            html.required.set(false)
        }
    }

    tasks.named<JacocoReport>("jacocoTestReport") {
        dependsOn(tasks.named("test"))
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    if (name != "narrativetrace-benchmarks" && name != "narrativetrace-build-tests" && name != "narrativetrace-gradle-plugin") {
        tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
            dependsOn(tasks.named("test"))
            val threshold = if (project.name == "narrativetrace-agent") "0.97" else "0.98"
            violationRules {
                rule {
                    enabled = true
                    element = "BUNDLE"
                    limit {
                        counter = "LINE"
                        value = "COVEREDRATIO"
                        minimum = threshold.toBigDecimal()
                    }
                }
            }
            if (project.name == "narrativetrace-examples") {
                classDirectories.setFrom(classDirectories.files.map { dir ->
                    fileTree(dir) {
                        exclude(
                            "**/ECommerceExample.class",
                            "**/LibraryExample*.class",
                            "**/PlantUmlImageRenderer.class",
                            "**/ClarityDemoExample.class"
                        )
                    }
                })
            }
        }

        tasks.named("check") {
            dependsOn(tasks.named("jacocoTestCoverageVerification"))
        }
    }

    if (name in setOf("narrativetrace-core", "narrativetrace-proxy", "narrativetrace-clarity")) {
        apply(plugin = "info.solidsoft.pitest")
        configure<info.solidsoft.gradle.pitest.PitestPluginExtension> {
            pitestVersion = "1.17.4"
            junit5PluginVersion = "1.2.1"
            threads = 4
            outputFormats = setOf("HTML", "XML")
            timestampedReports = false
            timeoutConstInMillis = 8000
            mutators = setOf("DEFAULTS")
            mutationThreshold = 65
        }
    }
}
