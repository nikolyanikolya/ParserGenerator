import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.simonharrer.graphviz") version "0.0.1"
    kotlin("jvm") version "1.7.20"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.graphstream:gs-core:1.3")
    implementation("org.graphstream:gs-algo:1.3")
    implementation("org.graphstream:gs-ui:1.3")
    testImplementation(kotlin("test"))
}

apply(plugin = "com.simonharrer.graphviz")

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}