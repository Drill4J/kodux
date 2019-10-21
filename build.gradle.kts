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
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:0.13.0")
            }
        }
        named("jvmMain") {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
                implementation(kotlin("reflect"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.2")
                implementation("org.jetbrains.xodus:xodus-entity-store:1.3.91")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.13.0")
                implementation("org.slf4j:slf4j-api:1.7.25")
                implementation("ch.qos.logback:logback-classic:1.2.3")
                implementation("ch.qos.logback:logback-core:1.2.3")
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