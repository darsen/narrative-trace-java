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

    group = "ai.narrativetrace"
    version = "0.1.0-SNAPSHOT"

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
}
