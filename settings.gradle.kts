plugins {
    id("com.gradleup.nmcp.settings").version("1.4.4")
}

val secrets = java.util.Properties().apply {
    val file = file("secret.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

nmcpSettings {
    centralPortal {
        username = secrets.getProperty("mavenCentralUsername")
        password = secrets.getProperty("mavenCentralPassword")
        publishingType = "USER_MANAGED"
    }
}

rootProject.name = "narrativetrace-java"

include("narrativetrace-core")
include("narrativetrace-proxy")
include("narrativetrace-junit5")
include("narrativetrace-examples")
include("narrativetrace-clarity")
include("narrativetrace-diagrams")
include("narrativetrace-slf4j")
include("narrativetrace-agent")
include("narrativetrace-spring")
include("narrativetrace-benchmarks")
include("narrativetrace-micrometer")
include("narrativetrace-junit4")
include("narrativetrace-junit4-example")
include("narrativetrace-build-tests")
include("narrativetrace-gradle-plugin")
include("narrativetrace-servlet")
include("narrativetrace-spring-web")
