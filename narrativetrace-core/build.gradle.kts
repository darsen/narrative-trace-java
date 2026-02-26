// TODO: secrets setup — gradle.properties with sonatypeCentralUsername/sonatypeCentralPassword,
//  GPG key ref, .gitignore, git-secret init/encrypt. Then bump version to 0.1.0 and publish.

plugins {
    `maven-publish`
    signing
}

java {
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.assertj:assertj-core:3.27.3")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name.set("NarrativeTrace Core")
                description.set("Auto-generates human-readable execution traces from method and parameter names. Zero runtime dependencies.")
                url.set("https://narrativetrace.ai")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("danijel")
                        name.set("Danijel Arsenovski")
                        email.set("danijel.arsenovski@empoweragile.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://gitlab.com/empower-agile/narrative-trace/narrative-trace-java.git")
                    developerConnection.set("scm:git:ssh://gitlab.com/empower-agile/narrative-trace/narrative-trace-java.git")
                    url.set("https://gitlab.com/empower-agile/narrative-trace/narrative-trace-java")
                }
            }
        }
    }
    repositories {
        maven {
            name = "sonatypeCentral"
            url = uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
            credentials {
                username = findProperty("sonatypeCentralUsername") as String? ?: ""
                password = findProperty("sonatypeCentralPassword") as String? ?: ""
            }
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications["mavenJava"])
}

tasks.withType<Sign>().configureEach {
    onlyIf { project.hasProperty("sonatypeCentralUsername") }
}
