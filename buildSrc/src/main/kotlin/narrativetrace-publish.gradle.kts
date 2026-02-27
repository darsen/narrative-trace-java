plugins {
    `maven-publish`
    signing
}

extensions.configure<JavaPluginExtension> {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name.set(provider { project.extra["publishName"] as String })
                description.set(provider { project.extra["publishDescription"] as String })
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
                    connection.set("scm:git:git://github.com/darsen/narrative-trace-java.git")
                    developerConnection.set("scm:git:ssh://github.com/darsen/narrative-trace-java.git")
                    url.set("https://github.com/darsen/narrative-trace-java")
                }
            }
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications["mavenJava"])
}

tasks.withType<Sign>().configureEach {
    onlyIf { rootProject.file("secret.properties").exists() }
}
