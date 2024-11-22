plugins {
    application
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "2.0.20"

    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
}

group = "org.example"
version = "1.0-SNAPSHOT"
val exposedVersion = "0.56.0"
val ktorVersion = "3.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-call-logging-jvm:3.0.1")
    implementation("io.ktor:ktor-server-core-jvm:3.0.1")
    implementation("io.ktor:ktor-server-pebble-jvm:3.0.1")
    implementation("io.ktor:ktor-server-core-jvm:3.0.1")
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:1.7.3")

    implementation("org.jetbrains.exposed", "exposed-core", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-dao", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-jdbc", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-java-time", exposedVersion)

    implementation("org.xerial:sqlite-jdbc:3.44.1.0")

    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-websockets:$ktorVersion")
    implementation("io.ktor:ktor-server-pebble:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")
    implementation("org.slf4j:slf4j-simple:2.1.0-alpha1")
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
