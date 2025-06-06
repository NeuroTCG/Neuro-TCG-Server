plugins {
    application
    kotlin("jvm") version "2.1.20"
    kotlin("plugin.serialization") version "2.1.20"

    id("org.jlleitschuh.gradle.ktlint") version "12.2.0"
}

ktlint {
    version = "1.5.0"
}

group = "org.example"
version = null
val exposedVersion = "0.60.0"
val ktorVersion = "3.1.1"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin", "kotlin-test")

    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.10.1")
    implementation("org.jetbrains.kotlinx", "kotlinx-serialization-json", "1.8.0")
    implementation("org.jetbrains.kotlinx", "kotlinx-serialization-cbor", "1.8.0")

    implementation("org.jetbrains.exposed", "exposed-core", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-dao", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-jdbc", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-java-time", exposedVersion)

    implementation("org.xerial", "sqlite-jdbc", "3.49.1.0")

    implementation("io.ktor", "ktor-server-auth", ktorVersion)
    implementation("io.ktor", "ktor-server-netty", ktorVersion)
    implementation("io.ktor", "ktor-server-content-negotiation", ktorVersion)
    implementation("io.ktor", "ktor-server-cors", ktorVersion)
    implementation("io.ktor", "ktor-server-websockets", ktorVersion)
    implementation("io.ktor", "ktor-server-pebble", ktorVersion)
    implementation("io.ktor", "ktor-serialization-kotlinx", ktorVersion)
    implementation("io.ktor", "ktor-serialization-kotlinx-json", ktorVersion)
    implementation("io.ktor", "ktor-client-core", ktorVersion)
    implementation("io.ktor", "ktor-client-okhttp", ktorVersion)
    implementation("io.ktor", "ktor-client-content-negotiation", ktorVersion)

    implementation("io.ktor", "ktor-server-call-logging-jvm", ktorVersion)
    implementation("io.ktor", "ktor-server-core-jvm", ktorVersion)
    implementation("io.ktor", "ktor-server-pebble-jvm", ktorVersion)
    implementation("io.ktor", "ktor-server-core-jvm", ktorVersion)

    implementation("com.squareup.okhttp3", "okhttp", "4.12.0")
    implementation("io.github.cdimascio", "dotenv-kotlin", "6.5.1")
    implementation("org.slf4j", "slf4j-simple", "2.0.17")
}

application {
    // REMOVE ME
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=true")

    mainClass.set("MainKt")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}
