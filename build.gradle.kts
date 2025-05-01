plugins {
    kotlin("jvm") version "2.1.10" apply false
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    group = "com.saggiodev"
    version = "0.1.0"

    repositories {
        mavenCentral()
    }
}

