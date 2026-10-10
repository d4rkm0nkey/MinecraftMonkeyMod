plugins {
    id("multiloader-common")
    alias(libs.plugins.moddevgradle)
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
