val ktor_version = "2.3.4"
val kotlin_version = "1.9.0"
val logback_version = "1.4.11"

plugins {
    kotlin("jvm") version "1.9.0"
    application
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.0"
}

group = "com.kotlinbank"
version = "0.0.1"

application {
    mainClass.set("com.kotlinbank.ApplicationKt")
}

// Forcer Java 17 au lieu de 23
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Ktor Server Core
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")

    // JSON Serialization
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version")
    implementation("io.ktor:ktor-serialization-gson-jvm:$ktor_version")

    // CORS (pour ton app Android)
    implementation("io.ktor:ktor-server-cors-jvm:$ktor_version")

    // Logging
    implementation("ch.qos.logback:logback-classic:$logback_version")

    // Tests
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")

}