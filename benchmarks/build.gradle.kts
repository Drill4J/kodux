import kotlinx.benchmark.gradle.*
import org.jetbrains.kotlin.allopen.gradle.*
import org.jetbrains.kotlin.gradle.tasks.*

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    kotlin("plugin.allopen")
    id("org.jetbrains.kotlinx.benchmark")
    id("org.jetbrains.kotlin.plugin.noarg")
    `maven-publish`
    idea
}

val benchmarkVersion: String by project
val xodusVersion: String by project
val coroutinesVersion: String by project
val serializationVersion: String by extra
val zstVersion: String by extra

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(project(":"))
    implementation("org.jetbrains.xodus:xodus-entity-store:$xodusVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:$benchmarkVersion")
    implementation("com.github.luben:zstd-jni:$zstVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$serializationVersion")
    testImplementation(kotlin("test-junit"))
}


configure<AllOpenExtension> { annotation("org.openjdk.jmh.annotations.State") }

tasks.withType<KotlinCompile> { kotlinOptions.jvmTarget = "1.8" }


benchmark {
    configurations {
        named("main") {
            iterationTime = 5
            iterationTimeUnit = "sec"

        }
    }
    targets {
        register("main") {
            this as JvmBenchmarkTarget
            jmhVersion = "1.21"
        }
    }
}

tasks.withType<JavaExec> {
    jvmArgs = listOf("-Xmx5g")
}
