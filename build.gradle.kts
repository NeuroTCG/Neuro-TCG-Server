import korlibs.korge.gradle.*

plugins {
	alias(libs.plugins.korge)
    `java-library`
}

korge {
	id = "com.sample.demo"

// To enable all targets at once

	//targetAll()

// To enable targets based on properties/environment variables
	//targetDefault()

// To selectively enable targets
	
	targetJvm()
	//targetJs()
    //targetWasm()
	//targetDesktop()
	//targetIos()
	//targetAndroid // Do not enable Android. Incompatible with Java plugin.

	serializationJson()
}

dependencies {
    add("commonMainApi", project(":deps"))
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-websockets-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-netty:3.2.9")
    implementation("io.ktor:ktor-websockets:3.2.9")
    testImplementation("io.ktor:ktor-server-tests-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.8")
    //add("commonMainApi", project(":korge-dragonbones"))
}

configureAutoVersions()

allprojects { repositories { mavenLocal(); mavenCentral(); google(); gradlePluginPortal() } }

