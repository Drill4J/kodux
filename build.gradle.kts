import java.net.*

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.github.hierynomus.license")
    `maven-publish`
}

val scriptUrl: String by extra

apply(from = "$scriptUrl/git-version.gradle.kts")

repositories {
    mavenLocal()
    apply(from = "$scriptUrl/maven-repo.gradle.kts")
    jcenter()
}

val coroutinesVersion: String by project
val serializationVersion: String by project
val xodusVersion: String by project
val zstVersion: String by project
val apacheCompressVersion: String by project
val fstVersion: String by project

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("de.ruedigermoeller:fst:$fstVersion")
    implementation("org.apache.commons:commons-compress:$apacheCompressVersion")
    implementation("com.github.luben:zstd-jni:$zstVersion")
    implementation("org.jetbrains.xodus:xodus-entity-store:$xodusVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
    testImplementation(kotlin("test-junit"))
}

java.targetCompatibility = JavaVersion.VERSION_1_8

kotlin {
    target {
        compilations.all { kotlinOptions.jvmTarget = "${project.java.targetCompatibility}" }
    }
    listOf(
        "kotlin.Experimental",
        "kotlinx.serialization.ImplicitReflectionSerializer",
        "kotlinx.serialization.InternalSerializationApi",
        "kotlinx.serialization.ExperimentalSerializationApi"
    ).let { annotations ->
        sourceSets.all { annotations.forEach(languageSettings::useExperimentalAnnotation) }
    }
}

publishing {
    publications {
        create<MavenPublication>("lib") {
            from(components["java"])
        }
    }
}

val licenseFormatSettings by tasks.registering(com.hierynomus.gradle.license.tasks.LicenseFormat::class) {
    source = fileTree(project.projectDir).also {
        include("**/*.kt", "**/*.java", "**/*.groovy")
        exclude("**/.idea")
    }.asFileTree
    headerURI = URI("https://raw.githubusercontent.com/Drill4J/drill4j/develop/COPYRIGHT")
}

license {
    skipExistingHeaders = true
}

tasks["licenseFormat"].dependsOn(licenseFormatSettings)
