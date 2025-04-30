plugins {
    application
}

application {
    mainClass.set("com.saggiodev.kastle.engine.MainKt")
}

dependencies {
    implementation(project(":api"))
    implementation("com.github.ajalt.clikt:clikt:5.0.1")
    implementation("com.varabyte.kotter:kotter-jvm:1.2.1")
    implementation("io.arrow-kt:arrow-core:2.1.0")
}
