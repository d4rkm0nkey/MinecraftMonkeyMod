# Minecraft "Cheeky Monkeys" Mod
Monkeys and bananas for your jungles, on Fabric and NeoForge.

![build workflow](https://github.com/d4rkm0nkey/MinecraftMonkeyMod/actions/workflows/build.yml/badge.svg)
<a href="https://discord.gg/d9cwBkwbe6" rel="some text">![Discord](https://img.shields.io/discord/823189930221240390)</a>
![Latest Release](https://img.shields.io/github/v/release/d4rkm0nkey/MinecraftMonkeyMod?include_prereleases)

## Features
- **Monkeys** live in troops in jungle biomes. They chatter, pound their chests, scratch their heads, rub their bellies and look around.
- **Bananas** hang from jungle trunks just below the canopy and ripen in three stages. You can eat them, or plant them on a jungle log like cocoa beans.
- **Taming:** feed a wild monkey bananas until it trusts you. Bananas also make monkeys fall in love, so wild troops can be bred.
- **Pets:** tamed monkeys follow you and sit on command. They eat from your hand, cheer when tamed and dance next to a playing jukebox.
- Monkeys are built for falling out of trees, so they take only a little fall damage.

## Requirements
| | Version |
|---|---|
| Minecraft | 26.3 |
| Fabric | Loader 0.19.5+, Fabric API |
| NeoForge | 26.3.0.66-beta+ |
| GeckoLib | 5.5.7+ (required on both loaders) |

## Development
The project is split into three Gradle subprojects:

| Project | Contents |
|---|---|
| `common` | All gameplay, rendering and assets, written against vanilla Minecraft (Mojang names) |
| `fabric` | Fabric entry points, Fabric API hooks and the client game test |
| `neoforge` | NeoForge entry points, events and biome modifiers |

Loader-specific registration goes through the `Platform` service (`common/.../platform/Platform.java`).

Requires JDK 25.

```sh
./gradlew build                     # jars in fabric/build/libs and neoforge/build/libs
./gradlew :fabric:runClient         # or :neoforge:runClient
./gradlew :neoforge:runClient -PquickPlayWorld=<save>   # jump straight into a world
./gradlew :fabric:runClientGameTest # spawns monkeys in every state and saves screenshots
                                    # to fabric/build/run/clientGameTest/screenshots
```

Animations live in `common/src/main/resources/assets/cheeky_monkeys/geckolib/animations/entity/monkey.animation.json`
and can be edited in Blockbench with the GeckoLib plugin.
