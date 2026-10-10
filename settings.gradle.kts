pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") {
            name = "Fabric"
            content {
                includeGroupAndSubgroups("net.fabricmc")
                includeGroup("fabric-loom")
            }
        }
        maven("https://maven.neoforged.net/releases") {
            name = "NeoForge"
            content { includeGroupAndSubgroups("net.neoforged") }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "cheeky-monkeys"

include("common", "fabric", "neoforge")
