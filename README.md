# Minecraft "Cheeky Monkeys" Mod
Monkeys and bananas for your jungles, on Fabric and NeoForge.

![build workflow](https://github.com/d4rkm0nkey/MinecraftMonkeyMod/actions/workflows/build.yml/badge.svg)
<a href="https://discord.gg/d9cwBkwbe6" rel="some text">![Discord](https://img.shields.io/discord/823189930221240390)</a>
![Latest Release](https://img.shields.io/github/v/release/d4rkm0nkey/MinecraftMonkeyMod?include_prereleases)

## Features
- **Monkeys** live in troops in jungle biomes, and grey, red-faced **snow monkeys** in snowy taigas and groves (they don't mind powder snow). They chatter, pound their chests, scratch their heads, rub their bellies and look around.
- **Bananas** hang from jungle trunks just below the canopy and ripen in three stages. You can eat them, or plant them on a jungle log like cocoa beans.
- **Taming:** feed a wild monkey bananas until it trusts you. Bananas also make monkeys fall in love, so wild troops can be bred.
- **Pets:** tamed monkeys eat from your hand, cheer when tamed and dance next to a playing jukebox. Right-click a pet to switch what it does:
  - **Follow:** stays with you, keeps a lookout (screeches and makes monsters coming for you glow) and snatches weapons out of monsters' hands to bring to you
  - **Stay:** sits where it is
  - **Wander:** roams and climbs around the spot you left it at
  - **Gather:** follows you and fetches items lying around (drops from mining, mobs or explosions) into your inventory
- **Shoulder rides:** sneak and right-click your monkey with an empty hand to carry it on your shoulder. Sneak again to let it down.
- **Up in the trees:** monkeys climb up trunks, swing hand over hand along under the canopy, climb up the side of the leaves and sit on top. They leap from tree to tree, hang from the leaves and climb or jump back down. A hurt monkey flees up the nearest tree, and if you stand in a tree holding a banana, monkeys climb up to you.
- **Banana thieves:** throw a banana and the monkeys race for it, grab it and run off to eat it in peace.
- **Pickpockets:** wild monkeys sneak up behind you and steal from your pockets, then run up a tree and hide the loot in a **stash nest**. Find the nest and you can open it and take everything back. Catch them looking and they pretend nothing happened. To get your things back, give the thief a smack or offer it a banana; a monkey always drops what it stole when it dies.
- **Troops:** wild monkeys stick together, and when one gets hurt it screeches and the whole troop scatters up the trees.
- **Leaping:** monkeys jump over gaps they can't walk around, and sometimes take a big leap forward just for fun.
- Monkeys are built for falling out of trees, so they take only a little fall damage.

- **Advancements:** six of them in the Husbandry tab, from eating a banana to finding a monkey's stash.

## Configuration
`config/cheeky_monkeys.json` is created on first start:

| Option | Default | |
|---|---|---|
| `pickpocketing` | `true` | Wild monkeys steal from players |
| `maxItemsStolen` | `4` | Most items taken from one stack |
| `stashNests` | `true` | Thieves hide loot in nests; otherwise they keep holding it |
| `grabThrownBananas` | `true` | Monkeys race for bananas on the ground |
| `petLookout` | `true` | Pets make monsters coming for you glow |
| `petDisarming` | `true` | Pets snatch weapons from monsters |

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

### Releasing
Versions are published to Modrinth and CurseForge as betas with the [mod publish plugin](https://modmuss50.github.io/mod-publish-plugin/):

1. Set `mod_version` and the project ids in `gradle.properties`, and write `CHANGELOG.md`
2. `./gradlew publishMods -PpublishDryRun` shows what would be uploaded
3. `MODRINTH_TOKEN=... CURSEFORGE_TOKEN=... ./gradlew publishMods` uploads. In CI, start the manual **publish** job on GitLab or the **publish** workflow on GitHub; both read the tokens from `MODRINTH_TOKEN` and `CURSEFORGE_TOKEN` variables/secrets

Animations live in `common/src/main/resources/assets/cheeky_monkeys/geckolib/animations/entity/monkey.animation.json`
and can be edited in Blockbench with the GeckoLib plugin.
