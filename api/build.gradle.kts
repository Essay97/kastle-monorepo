plugins {
    id("app.cash.sqldelight") version "2.0.2"
    id("io.deepmedia.tools.deployer") version "0.16.0"
}

repositories {
    google()
    mavenCentral()
}

sqldelight {
    databases {
        create("Database") {
            packageName.set("com.saggiodev.kastle.db")
            srcDirs("sqldelight")
        }
    }
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
    implementation("io.arrow-kt:arrow-core:2.1.0")
    implementation("app.cash.sqldelight:sqlite-driver:2.0.2")
}

publishing {
    publications {
        create<MavenPublication>("kastle-api") {
            from(components["java"])
            artifactId = "api"
        }
    }

    repositories {
        maven {
            name = "local"
            url = uri(System.getProperty("user.home") + "/localMavenRepo")
        }
    }
}


deployer {
    content {
        kotlinComponents()
    }
    projectInfo {
        description = "Core logic and DSL for the Kastle text adventure engine"
        url = "https://github.com/Essay97/kastle-monorepo"
        scm.fromGithub("Essay97", "kastle-monorepo")
        license(apache2)
        developer("Enrico Saggiorato", "saggiorato.enrico@gmail.com")
        groupId = "com.saggiodev"
    }
    centralPortalSpec {
        signing.key = secret("SIGNING_KEY")
        signing.password = secret("SIGNING_PASSPHRASE")
        auth.user = secret("UPLOAD_USERNAME")
        auth.password = secret("UPLOAD_PASSWORD")
    }
}
