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

val coroutinesVersion: String by project
val serializationVersion: String by project
val xodusVersion: String by project

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.xodus:xodus-entity-store:$xodusVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
    testImplementation(kotlin("test-junit"))
}



tasks {

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlin.Experimental"
        kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlinx.serialization.ImplicitReflectionSerializer"
        kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlinx.serialization.InternalSerializationApi"
        kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlinx.serialization.ExperimentalSerializationApi"
    }

}

publishing {
    repositories {
        maven {
            url = uri("http://oss.jfrog.org/oss-release-local")
            credentials {
                username =
                    if (project.hasProperty("bintrayUser"))
                        project.property("bintrayUser").toString()
                    else System.getenv("BINTRAY_USER")
                password =
                    if (project.hasProperty("bintrayApiKey"))
                        project.property("bintrayApiKey").toString()
                    else System.getenv("BINTRAY_API_KEY")
            }
        }
    }
    publications {
        create<MavenPublication>("lib") {
            from(components["java"])
        }
    }
}
