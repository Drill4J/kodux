@file:Suppress("UnstableApiUsage")

rootProject.name = "kodux"
pluginManagement {
    val kotlinVersion: String by settings
    val benchmarkVersion: String by settings
    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
        kotlin("plugin.allopen") version kotlinVersion
        id("kotlinx.benchmark") version benchmarkVersion
    }

    repositories {
        gradlePluginPortal()
        maven(url = "https://oss.jfrog.org/artifactory/list/oss-release-local")
        maven(url = "https://dl.bintray.com/kotlin/kotlinx/")
    }
}

include(":benchmarks")