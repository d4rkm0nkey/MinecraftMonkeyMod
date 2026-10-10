// Shared configuration for the common, fabric and neoforge projects.
// Loader projects compile the common sources and resources into their own jar.
plugins {
    java
}

fun prop(name: String) = project.property(name) as String
val modId = prop("mod_id")
val modName = prop("mod_name")
val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
val mcVersion = catalog.findVersion("minecraft").get().requiredVersion
val javaVersion = catalog.findVersion("java").get().requiredVersion.toInt()

version = "${prop("mod_version")}+$mcVersion"
group = prop("mod_group")
base.archivesName = "$modId-${project.name}"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
    withSourcesJar()
}

repositories {
    maven("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/") {
        name = "GeckoLib"
        content { includeGroupAndSubgroups("com.geckolib") }
    }
    mavenCentral()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = javaVersion
}

tasks.withType<Jar>().configureEach {
    from(rootProject.file("LICENSE")) { rename { "${it}_$modId" } }
}

tasks.withType<ProcessResources>().configureEach {
    val props = mapOf(
        "version" to project.version.toString(),
        "mod_id" to modId,
        "mod_name" to modName,
        "mod_authors" to prop("mod_authors"),
        "mod_license" to prop("mod_license"),
        "mod_description" to prop("mod_description"),
        "minecraft_version" to mcVersion,
        "java_version" to javaVersion,
        "fabric_loader_version" to catalog.findVersion("fabric-loader").get().requiredVersion,
        "fabric_api_version" to catalog.findVersion("fabric-api").get().requiredVersion,
        "neoforge_version" to catalog.findVersion("neoforge").get().requiredVersion,
        "geckolib_version" to catalog.findVersion("geckolib").get().requiredVersion,
    )
    inputs.properties(props)
    filesMatching(listOf("fabric.mod.json", "META-INF/neoforge.mods.toml", "*.mixins.json")) {
        expand(props)
    }
}

if (project.name != "common") {
    val commonMain = project(":common").extensions.getByType<SourceSetContainer>().getByName("main")

    tasks.named<JavaCompile>("compileJava") { source(commonMain.allJava) }
    tasks.named<ProcessResources>("processResources") { from(commonMain.resources) }
    tasks.named<Jar>("sourcesJar") { from(commonMain.allSource) }
}
