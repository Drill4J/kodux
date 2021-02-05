rootProject.name = "kodux"

val scriptUrl: String by extra
apply(from = "$scriptUrl/maven-repo.settings.gradle.kts")

pluginManagement {
    val kotlinVersion: String by settings
    val benchmarkVersion: String by settings
    val licenseVersion: String by extra
    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
        kotlin("plugin.allopen") version kotlinVersion
        id("kotlinx.benchmark") version benchmarkVersion
        id("com.github.hierynomus.license") version licenseVersion
    }

    repositories {
        maven("https://dl.bintray.com/kotlin/kotlinx")
    }
}

include(":benchmarks")
