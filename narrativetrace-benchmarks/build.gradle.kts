plugins {
    id("me.champeau.jmh") version "0.7.3"
}

dependencies {
    jmh(project(":narrativetrace-core"))
    jmh(project(":narrativetrace-proxy"))
    jmh(project(":narrativetrace-spring"))
    jmh(project(":narrativetrace-agent"))
    jmh("org.springframework:spring-context:6.2.3")
}

val agentJar = project(":narrativetrace-agent").tasks.named<Jar>("jar").map { it.archiveFile.get().asFile.absolutePath }

val jmhProfilers = (findProperty("jmhProfilers") as? String)?.split(",") ?: emptyList()

jmh {
    warmupIterations.set(3)
    iterations.set(5)
    fork.set(1)
    timeOnIteration.set("1s")
    warmup.set("1s")
    jvmArgs.set(listOf("-Xms256m", "-Xmx256m"))
    jvmArgsAppend.set(agentJar.map { listOf("-javaagent:$it=packages=ai.narrativetrace.benchmarks.agent") })
    if (jmhProfilers.isNotEmpty()) {
        profilers.set(jmhProfilers)
    }
}
