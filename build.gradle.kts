import org.jetbrains.kotlin.gradle.tasks.*

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    `maven-publish`
    idea
}

apply(from = "gradle/git-version.gradle.kts")

repositories {
    mavenLocal()
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.4")
    implementation("org.jetbrains.xodus:xodus-entity-store:1.3.91")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.20.0")
    testImplementation(kotlin("test-junit"))
}



tasks {

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlin.Experimental"
        kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlinx.serialization.ImplicitReflectionSerializer"
        kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlinx.serialization.InternalSerializationApi"
    }

}
