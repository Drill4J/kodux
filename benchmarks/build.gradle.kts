import kotlinx.benchmark.gradle.*
import org.jetbrains.kotlin.allopen.gradle.*
import org.jetbrains.kotlin.gradle.tasks.*

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    kotlin("plugin.allopen")
    id("kotlinx.benchmark")
    `maven-publish`
    idea
}

repositories {
    mavenLocal()
    mavenCentral()
    jcenter()
    maven(url = "https://dl.bintray.com/kotlin/kotlinx")
}

val coroutinesVersion: String by project
val serializationVersion: String by project
val xodusVersion: String by project
val benchmarkVersion: String by project

dependencies {
    implementation(project(":"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.xodus:xodus-entity-store:$xodusVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")
    implementation("org.jetbrains.kotlinx:kotlinx.benchmark.runtime-jvm:$benchmarkVersion")
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