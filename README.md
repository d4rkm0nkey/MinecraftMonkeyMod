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
- **Up in the trees:** monkeys climb up trunks, swing hand over hand along under the canopy, climb up the side of the leaves and sit on top. They leap from tree to tree, hang from the leaves and climb or jump back down. A hurt monkey flees up the nearest tree, and if you stand in a tree holding a banana, monkeys climb up to you.
- **Leaping:** monkeys jump over gaps they can't walk around, and sometimes take a big leap forward just for fun.
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
./gradlew :fabric:runClientGameTest # shows monkeys in every state, climbing and leaping, and saves screenshots
                                    # to fabric/build/run/clientGameTest/screenshots
```

Animations live in `common/src/main/resources/assets/cheeky_monkeys/geckolib/animations/entity/monkey.animation.json`
and can be edited in Blockbench with the GeckoLib plugin.
