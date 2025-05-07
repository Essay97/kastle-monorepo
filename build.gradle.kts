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
}

tasks.register("appVersion") {
    doLast {
        println(rootProject.version)
    }
}

