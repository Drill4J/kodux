import org.jetbrains.kotlin.gradle.tasks.*

plugins {
    `kotlin-multiplatform`
    `kotlinx-serialization`
    `maven-publish`
    idea
}
apply(from = "https://raw.githubusercontent.com/Drill4J/build-scripts/master/publish.gradle")
repositories {
    mavenLocal()
    mavenCentral()
    jcenter()

}

kotlin {
    jvm()
    sourceSets {
        named("commonMain") {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:0.14.0")
            }
        }
        named("jvmMain") {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
                implementation(kotlin("reflect"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.2-1.3.60")
                implementation("org.jetbrains.xodus:xodus-entity-store:1.3.91")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.14.0")
            }
        }
        named("jvmTest") {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
    }
}


tasks {

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlin.Experimental"
        kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlinx.serialization.ImplicitReflectionSerializer"
        kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlinx.serialization.InternalSerializationApi"
    }

}