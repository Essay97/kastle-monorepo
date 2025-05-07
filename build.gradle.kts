import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    kotlin("jvm") version "2.1.10" apply false
}

group = "com.saggiodev"
version = "0.1.1"

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
    }

    extensions.configure<KotlinJvmProjectExtension> {
        jvmToolchain(11)
    }
}

tasks.register("appVersion") {
    doLast {
        println(rootProject.version)
    }
}

