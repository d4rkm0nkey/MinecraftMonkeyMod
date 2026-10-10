plugins {
    id("multiloader-common")
    alias(libs.plugins.loom)
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
