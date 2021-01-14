plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
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

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
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
