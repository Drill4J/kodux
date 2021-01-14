plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    kotlin("plugin.allopen")
    id("kotlinx.benchmark")
    idea
}

repositories {
    mavenLocal()
    jcenter()
    maven(url = "https://dl.bintray.com/kotlin/kotlinx")
}

val coroutinesVersion: String by project
val serializationVersion: String by project
val xodusVersion: String by project
val benchmarkVersion: String by project

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(project(":"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.xodus:xodus-entity-store:$xodusVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")
    implementation("org.jetbrains.kotlinx:kotlinx.benchmark.runtime-jvm:$benchmarkVersion")
    testImplementation(kotlin("test-junit"))
}

kotlin {
    target { compilations.all { kotlinOptions.jvmTarget = "1.8" } }
}

configure<org.jetbrains.kotlin.allopen.gradle.AllOpenExtension> { annotation("org.openjdk.jmh.annotations.State") }

benchmark {
    configurations {
        named("main") {
            iterationTime = 5
            iterationTimeUnit = "sec"

        }
    }
    targets {
        register("main") {
            this as kotlinx.benchmark.gradle.JvmBenchmarkTarget
            jmhVersion = "1.21"
        }
    }
}
