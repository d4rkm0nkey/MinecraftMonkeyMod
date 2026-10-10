plugins {
    id("multiloader-common")
    alias(libs.plugins.moddevgradle)
}

// Vanilla-only Minecraft for loader-agnostic code
neoForge {
    neoFormVersion = libs.versions.neoform.get()
}

dependencies {
    compileOnly(libs.geckolib.common)
    // GeckoLib injects GeoRenderState into vanilla render states
    interfaceInjectionData(libs.geckolib.common)
}
