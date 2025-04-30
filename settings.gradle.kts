rootProject.name = "kastle-monorepo"

include("api", "engine", "sample-game")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

