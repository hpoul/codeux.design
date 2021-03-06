import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/*
 * This file was generated by the Gradle 'init' task.
 *
 * This generated file contains a sample Kotlin application project to get you started.
 */

plugins {
    // Apply the Kotlin JVM plugin to add support for Kotlin.
    kotlin("jvm") version "1.4.0"
    id("io.ratpack.ratpack-java") version "1.8.0"

    // Apply the application plugin to add support for building a CLI application.
    application
}


if (gradle.startParameter.isContinuous) {
    tasks.named<ratpack.gradle.continuous.RatpackContinuousRun>("run") {
        flattenClassloaders = true
    }
}

repositories {
    // Use jcenter for resolving dependencies.
    // You can declare any Maven/Ivy/file repository here.
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://oss.sonatype.org/content/groups/public/")
    jcenter()
}

val dc2fVersion = "0.2.3-SNAPSHOT"

tasks.withType<KotlinCompile> {
    sourceCompatibility = "1.8"
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs = listOf("-Xjvm-default=enable")
}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.dc2f:dc2f:$dc2fVersion")
    implementation("com.dc2f:dc2f-common:$dc2fVersion")
    implementation("org.unbescape:unbescape:1.1.6.RELEASE")


    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Use the Kotlin JUnit integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

application {
    // Define the main class for the application
    mainClassName = "com.dc2f.starter.AppKt"
}
