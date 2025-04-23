plugins {
    kotlin("jvm") version "2.1.10" apply false
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    group = "com.example.textadventure"
    version = "1.0.0"

    repositories {
        mavenCentral()
    }

    dependencies {
        // puoi mettere qui dipendenze comuni se ce ne sono
    }
}

