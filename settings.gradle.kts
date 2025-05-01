rootProject.name = "kastle"

include("api", "engine", "sample-game")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

