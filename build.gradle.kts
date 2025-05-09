import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    kotlin("jvm") version "2.1.10" apply false
}

group = "com.saggiodev"

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    group = rootProject.group

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

