plugins {
    id("multiloader-common")
    alias(libs.plugins.moddevgradle)
    alias(libs.plugins.mod.publish)
}

val modId = property("mod_id") as String

neoForge {
    version = libs.versions.neoforge.get()

    runs {
        configureEach {
            gameDirectory = file("../runs/neoforge-$name")
        }
        register("client") {
            client()
            programArguments.addAll("--username", "Dev")
            // ./gradlew :neoforge:runClient -PquickPlayWorld=<save name> jumps straight into a world
            providers.gradleProperty("quickPlayWorld").orNull?.let { programArguments.addAll("--quickPlaySingleplayer", it) }
        }
        register("server") {
            server()
            programArgument("--nogui")
        }
        register("data") {
            clientData()
        }
    }

    mods {
        register(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
}

dependencies {
    implementation(libs.geckolib.neoforge)
    interfaceInjectionData(libs.geckolib.common)
    compileOnly(project(":common"))
}

// Let the dev server read console commands
tasks.matching { it.name == "runServer" }.configureEach {
    (this as JavaExec).standardInput = System.`in`
}

// Uploads the jar to Modrinth and CurseForge as a beta. `./gradlew publishMods -PpublishDryRun` checks everything without uploading.
publishMods {
    file = tasks.jar.flatMap { it.archiveFile }
    displayName = "Cheeky Monkeys ${project.version} (NeoForge)"
    version = "${project.version}-${project.name}"
    changelog = providers.fileContents(rootProject.layout.projectDirectory.file("CHANGELOG.md")).asText
    type = BETA
    modLoaders.add("neoforge")
    dryRun = providers.gradleProperty("publishDryRun").isPresent

    modrinth {
        projectId = providers.gradleProperty("modrinth_project_id")
        accessToken = providers.environmentVariable("MODRINTH_TOKEN")
        minecraftVersions.add(libs.versions.minecraft)
        requires("geckolib")
    }

    curseforge {
        projectId = providers.gradleProperty("curseforge_project_id")
        accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
        minecraftVersions.add(libs.versions.minecraft)
        javaVersions.add(JavaVersion.VERSION_25)
        client = true
        server = true
        requires("geckolib")
    }
}
