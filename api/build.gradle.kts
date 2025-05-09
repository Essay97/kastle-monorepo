plugins {
    id("app.cash.sqldelight") version "2.0.2"
    id("io.deepmedia.tools.deployer") version "0.18.0"
}

version = "0.1.1"

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
    implementation("org.slf4j:slf4j-nop:1.7.36")
}

deployer {
    content {
        kotlinComponents {
            emptyDocs()
            kotlinSources()
        }

    }
    projectInfo {
        description = "Core logic and DSL for the Kastle text adventure engine"
        url = "https://github.com/Essay97/kastle-monorepo"
        scm.fromGithub("Essay97", "kastle-monorepo")
        license(apache2)
        developer("Enrico Saggiorato", "saggiorato.enrico@gmail.com")
        groupId = "com.saggiodev"
        artifactId = "kastle-api"
    }
    centralPortalSpec {
        signing.key = secret("SIGNING_KEY")
        signing.password = secret("SIGNING_PASSPHRASE")
        auth.user = secret("UPLOAD_USERNAME")
        auth.password = secret("UPLOAD_PASSWORD")
    }
    localSpec {
        directory = file("/Users/enrico/maven-local")
    }
}
