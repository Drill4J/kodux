rootProject.name = "kodux"

val scriptUrl: String by extra
apply(from = "$scriptUrl/maven-repo.settings.gradle.kts")

pluginManagement {
    val kotlinVersion: String by settings
    val benchmarkVersion: String by settings
    val licenseVersion: String by extra
    val kotlinNoarg: String by extra
    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
        id("org.jetbrains.kotlinx.benchmark") version benchmarkVersion
        kotlin("plugin.allopen") version kotlinVersion
        id("com.github.hierynomus.license") version licenseVersion
        id("org.jetbrains.kotlin.plugin.noarg") version kotlinNoarg
    }

    repositories {
        maven("https://dl.bintray.com/kotlin/kotlinx")
    }
}

include(":benchmarks")
