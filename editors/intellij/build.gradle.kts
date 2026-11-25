plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
}

group = "io.github.sirmspencer"
version = providers.gradleProperty("pluginVersion").get()

kotlin {
    jvmToolchain(21)
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity(providers.gradleProperty("platformVersion"))
    }
}

intellijPlatform {
    pluginConfiguration {
        version = providers.gradleProperty("pluginVersion")
        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = provider { null }
        }
    }
    // ./gradlew publishPlugin reads the marketplace token from PUBLISH_TOKEN
    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }
    pluginVerification {
        ides {
            // the installed ide, so verification does not download others
            local(file("/Applications/IntelliJ IDEA CE.app"))
        }
    }
}

tasks {
    // bin/<os>-<arch>/clj-kondo-fix binaries, filled by scripts/package-intellij.sh, ship inside the plugin
    prepareSandbox {
        from(layout.projectDirectory.dir("bin")) {
            into(intellijPlatform.projectName.map { "$it/bin" })
        }
    }
}
