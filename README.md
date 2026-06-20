<div align="center">

<img src="https://raw.githubusercontent.com/Dwinovo/minecraft-chiikawa/main/common/src/main/resources/logo.png" alt="Chiikawa Mod" width="320" />

# Chiikawa

**Bring Chiikawa, Hachiware, Usagi and friends into Minecraft — tameable pets with their own jobs, animations, and a working music box that plays your own songs.**

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1%20–%2026.1.2-62B47A?logo=minecraft&logoColor=white)](#supported-versions)
[![Loaders](https://img.shields.io/badge/Loaders-Fabric%20%7C%20NeoForge%20%7C%20Forge-blue)](#supported-versions)
[![Modrinth](https://img.shields.io/badge/Download-Modrinth-00AF5C?logo=modrinth&logoColor=white)](https://modrinth.com/mod/pT971QUb)
[![CurseForge](https://img.shields.io/badge/Download-CurseForge-F16436?logo=curseforge&logoColor=white)](https://www.curseforge.com/minecraft/mc-mods/chiikawa)
[![License](https://img.shields.io/badge/License-CC--BY--NC--SA--4.0-lightgrey)](LICENSE)

</div>

---

## Features

- 🐾 **Seven tameable pets** from the Chiikawa universe, each with its own model, textures, and personality.
- 🧰 **Jobs & modes** — set a pet to **Follow**, **Sit**, or **Work**, and give it a job: **Farmer**, **Fencer**, **Archer**, or **Musician**. Working pets act on their own.
- 🎬 **In-house animation engine** — a Bedrock model/animation pipeline built for this mod, inspired by [GeckoLib](https://github.com/bernie-g/geckolib) but with no GeckoLib runtime dependency, driven by a semantic state machine for smooth idle, walk, run, work, and reaction poses.
- 🎒 **Pet backpack** — right-click a pet to open its inventory and manage its gear and tools.
- 🎵 **Music box** — drop your own `.mp3` / `.wav` files into a folder and stream them in-game. Pure-Java decoding, **no external ffmpeg required**, with an in-screen "Open Folder" button.
- 🪆 **Pet dolls & spawn eggs** — a doll for every character (try placing one on a cake 🎂).
- ⚔️ **Character weapons** — craftable signature "discipline sticks" for Chiikawa, Hachiware, and Usagi.

## Download

| Platform | Link |
| --- | --- |
| **Modrinth** | https://modrinth.com/mod/pT971QUb |
| **CurseForge** | https://www.curseforge.com/minecraft/mc-mods/chiikawa |

> **Fabric** players also need the [Fabric API](https://modrinth.com/mod/fabric-api).

## How to Play

New here? This is everything you need — most "it's not working" reports come from missing one of these steps.

### 🍖 Taming
Hold **almost any food** (apple, bread, cookie, carrot, potato, cooked meat, melon slice, sweet berries, golden apple…) and **right-click the pet**.

> ⚠️ **Taming is chance-based.** Each feed has a chance to succeed, so keep feeding! **Hearts** = tamed. A **puff of smoke / confused reaction** = not yet, try again. Flowers no longer work — use food.

### 🧭 Modes — Follow / Sit / Work
Once it's yours, **Sneak (Shift) + right-click with an empty hand** to cycle through:

- **Follow** – the pet follows you.
- **Sit** – the pet stays put.
- **Work** – the pet performs its job around the spot where you set it.

### 💼 Jobs
A pet's job is decided by the **tool it holds**. Open its backpack (right-click with an empty hand), place a tool inside, then set it to **Work**:

| Job | Give it… | What it does |
| --- | --- | --- |
| **Farmer** | a Hoe | plants and harvests nearby crops |
| **Fencer** | a Sword (or a character's discipline stick) | attacks hostile mobs in melee |
| **Archer** | a Bow | attacks hostile mobs at range |
| **Musician** | a Music Box | plays your imported music |

> Pets only fight/work while in **Work** mode — in Follow mode they stay peaceful.

### ❤️ Other interactions
- **Heal** a hurt pet by right-clicking it with food.
- **Open its backpack** by right-clicking with an empty hand (no sneak).

### 🌍 Where they spawn
Pets appear naturally in **Plains, Sunflower Plains, Savanna, Savanna Plateau, Desert, Swamp, and Snowy Plains**. In Creative, use the spawn eggs.

### 🎁 Getting items
- **Weapons** (discipline sticks) are **craftable** — check the recipe book (wool + stick + flint).
- The **Music Box**, **dolls**, and **spawn eggs** are in the Creative inventory. A pet also **drops its doll when it dies**, preserving its backpack and data.

## Music Box

1. Open the **Music Box** screen and click **Open Folder** to jump to `config/chiikawa/music`.
2. Drop your `.mp3` or `.wav` files in, hit **Reload**, and pick a track.
3. Give the Music Box to a pet and set it to the **Musician** job to have it play for you.

Audio is decoded in pure Java (mp3spi / JLayer for MP3, the JDK for WAV), encoded with Opus, and streamed from the server to nearby clients — no external tools needed.

## Supported Versions

| Minecraft | Loaders |
| --- | --- |
| 26.1.2 | Fabric · NeoForge |
| 1.21.11 / 1.21.10 / 1.21.8 / 1.21.7 / 1.21.6 / 1.21.5 / 1.21.4 / 1.21.1 | Fabric · NeoForge |
| 1.20.6 | Fabric · NeoForge |
| 1.20.4 / 1.20.2 / 1.20.1 | Fabric · Forge |

Only Minecraft 1.20.1 and newer are maintained.

## Building from Source

The repo is a [MultiLoader](https://github.com/jaredlll08/MultiLoader-Template)-style project: shared code lives in `common`, and each loader compiles it directly. Each Minecraft version lives on its own git branch.

```bash
git checkout 1.21.1                                   # pick your version branch

./gradlew :fabric:runClient                           # run a dev client
./gradlew :neoforge:runClient                         # (:forge:runClient on 1.20.1–1.20.4)

./gradlew :fabric:runDatagen :neoforge:runData        # regenerate data
./gradlew build                                        # build all jars
```

Built jars land in `fabric/build/libs/` and `neoforge/build/libs/` (or `forge/build/libs/`). On Windows, use `gradlew.bat`. Use JDK 17 for 1.20.x, JDK 21 for 1.21.x, and JDK 25 for 26.1.2.

## Contributing

Issues and pull requests are welcome.

- Put shared logic in `common`; only loader-specific glue belongs in `fabric` / `neoforge` / `forge`.
- Run `./gradlew :common:test` before opening a PR.
- Generated resources under `**/generated/` are git-ignored and recreated by datagen — don't commit them.
- Each Minecraft version is a separate branch; target the branch your change applies to.

See [`AGENTS.md`](AGENTS.md) and [`docs/`](docs/) for design notes on the rendering pipeline and pet state machine.

## License & Credits

- Licensed under [**CC-BY-NC-SA-4.0**](LICENSE).
- Art support by **zoe_1000**.
- *Chiikawa* and its characters are the property of **Nagano**. This is a non-commercial fan project, not affiliated with or endorsed by the rights holders.
