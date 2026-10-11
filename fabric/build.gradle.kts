plugins {
    id("multiloader-common")
    alias(libs.plugins.loom)
    alias(libs.plugins.mod.publish)
}

dependencies {
    minecraft(libs.minecraft)
    implementation(libs.fabric.loader)
    implementation(libs.fabric.api)
    implementation(libs.geckolib.fabric)
    compileOnly(project(":common"))
}

loom {
    runs {
        configureEach {
            runDirectory = file("../runs/fabric-$name")
            generateRunConfig = true
        }
        named("client") {
            client()
            displayName = "Fabric Client"
        }
        named("server") {
            server()
            displayName = "Fabric Server"
        }
    }
}

fabricApi {
    configureTests {
        createSourceSet = true
        modId = "cheeky_monkeys_gametest"
        enableGameTests = false
        enableClientGameTests = true
        eula = true
    }
}

// Uploads the jar to Modrinth and CurseForge as a beta. `./gradlew publishMods -PpublishDryRun` checks everything without uploading.
publishMods {
    file = tasks.jar.flatMap { it.archiveFile }
    displayName = "Cheeky Monkeys ${project.version} (Fabric)"
    version = "${project.version}-${project.name}"
    changelog = providers.fileContents(rootProject.layout.projectDirectory.file("CHANGELOG.md")).asText
    type = BETA
    modLoaders.add("fabric")
    dryRun = providers.gradleProperty("publishDryRun").isPresent

    modrinth {
        projectId = providers.gradleProperty("modrinth_project_id")
        accessToken = providers.environmentVariable("MODRINTH_TOKEN")
        minecraftVersions.add(libs.versions.minecraft)
        requires("fabric-api", "geckolib")
    }

    curseforge {
        projectId = providers.gradleProperty("curseforge_project_id")
        accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
        minecraftVersions.add(libs.versions.minecraft)
        javaVersions.add(JavaVersion.VERSION_25)
        client = true
        server = true
        requires("fabric-api", "geckolib")
    }
}
