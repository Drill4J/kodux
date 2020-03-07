plugins {
    `kotlin-dsl`
}

repositories {
    maven(url = "https://dl.bintray.com/kotlin/kotlinx/")
    jcenter()
}

val kotlinVersion = "1.3.70"
dependencies {
    implementation(kotlin("gradle-plugin", kotlinVersion))
    implementation(kotlin("stdlib-jdk8", kotlinVersion))
    implementation(kotlin("serialization", kotlinVersion))
    implementation(kotlin("reflect", kotlinVersion))
}

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}