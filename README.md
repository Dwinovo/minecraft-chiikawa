<div align="center">

<img src="https://raw.githubusercontent.com/Dwinovo/minecraft-chiikawa/1.21.1/common/src/main/resources/logo.png" alt="Chiikawa Mod" width="320" />

# Chiikawa Mod

**Bring the world of *Chiikawa* into Minecraft — tameable little friends, a job system that lets them work on their own, and a music box that plays your very own songs.**

**English** · [简体中文](README_ZH.md)

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1%20–%2026.1.2-62B47A?logo=minecraft&logoColor=white)](#supported-versions)
[![Loaders](https://img.shields.io/badge/Loaders-Fabric%20%7C%20NeoForge%20%7C%20Forge-blue)](#supported-versions)
[![Modrinth](https://img.shields.io/badge/Download-Modrinth-00AF5C?logo=modrinth&logoColor=white)](https://modrinth.com/mod/pT971QUb)
[![CurseForge](https://img.shields.io/badge/Download-CurseForge-F16436?logo=curseforge&logoColor=white)](https://www.curseforge.com/minecraft/mc-mods/chiikawa)
[![License](https://img.shields.io/badge/License-CC--BY--NC--SA--4.0-lightgrey)](LICENSE)
[![GitHub Stars](https://img.shields.io/github/stars/Dwinovo/minecraft-chiikawa?style=flat&logo=github&label=Star)](https://github.com/Dwinovo/minecraft-chiikawa)

</div>

---

> [!IMPORTANT]
> ⭐ **Enjoying the mod? [Give it a Star](https://github.com/Dwinovo/minecraft-chiikawa)** — it takes one click and is the best way to show support.
> 🐛 **Found a bug or have an idea? [Open an issue](https://github.com/Dwinovo/minecraft-chiikawa/issues/new/choose)** — please use GitHub Issues rather than the Modrinth / CurseForge comments, so nothing gets lost.

> Chiikawa, Hachiware, Usagi… these healing, slightly goofy little ones now move into your world.
> Tame them, hand them a tool, and they'll farm, fight, and even **pick up a music box and play your favorite song** for you.
> They'll be adorable, they'll get hurt, they'll let out their signature squeaks — and when they fall, all it takes is a slice of cake to bring them back. 🎂

<!--
  📸 Gallery placeholder — strongly recommend dropping 2–4 screenshots / GIFs here.
  This is what decides whether people click download:
  - a pack of little ones following the player (shows models + animation)
  - a Farmer planting and harvesting in a field
  - Hachiware holding the music box and playing (with a "♪ Now Playing" subtitle)
  - the moment a doll is placed on a cake to revive
  Example:
  <div align="center">
    <img src="docs/img/farming.gif"  width="45%" />
    <img src="docs/img/music.gif"    width="45%" />
  </div>
-->

## ✨ Why you'll want it

- 🐾 **Seven characters from the original**, all tameable — Chiikawa, Hachiware, Usagi, Shisa, Momonga, Kurimanju, and Rakko, each with its own model, textures, and personality.
- 🧰 **A job system that works on its own** — drop a tool into a pet's backpack, switch it to **Free Roam**, and it gets busy for you: the **Farmer** plants and harvests, the **Fencer** fights up close, the **Archer** picks off enemies at range, and the **Musician** plays for you.
- 🎵 **A music box that plays your own songs** — drop `.mp3` / `.wav` files into a folder and hear them in-game. Pure-Java decoding, **no external ffmpeg required**; on a multiplayer server, everyone nearby hears the same track.
- 🔊 **Per-character sounds** — Usagi's signature squeaks, plus tame/hurt voices for the cast, to make them feel alive.
- 🎒 **Pet backpack** — right-click a pet with an empty hand to open its inventory and manage its tools and gear.
- 🪆 **Dolls & spawn eggs** — a doll for every character (try placing one on a cake 🎂), and spawn eggs in Creative.
- ⚔️ **Character weapons** — craftable signature "discipline sticks" for Chiikawa, Hachiware, and Usagi.

## 📥 Download

| Platform | Link |
| --- | --- |
| **Modrinth** | https://modrinth.com/mod/pT971QUb |
| **CurseForge** | https://www.curseforge.com/minecraft/mc-mods/chiikawa |

> **Fabric** players also need the [Fabric API](https://modrinth.com/mod/fabric-api).

## 🎮 How to Play

New here? This section is all you need — most "it's not working" reports come from skipping one of these steps.

### 🍖 Taming
Hold **almost any food** (apple, bread, cookie, carrot, potato, cooked meat, melon slice, sweet berries, golden apple…) and **right-click the pet**.

> ⚠️ **Taming is chance-based.** Each feed has a chance to succeed, so keep feeding! **Hearts** = tamed. A **puff of smoke / confused reaction** = not yet, try again. Flowers no longer work — use food.

### 🧭 Three modes — Follow / Sit / Free Roam
Once it's yours, **Sneak (Shift) + right-click with an empty hand** to cycle through:

- **Follow** — the pet follows you.
- **Sit** — the pet stays put.
- **Free Roam** — the pet performs its job around the spot where you set it.

### 💼 Jobs
A pet's job is decided by the **tool it holds**. Open its backpack (right-click with an empty hand), place a tool inside, then set it to **Free Roam**:

| Job | Give it… | What it does |
| --- | --- | --- |
| **Farmer** | a Hoe | plants and harvests nearby crops (modded seeds supported) |
| **Fencer** | a Sword (or a character's discipline stick) | attacks hostile mobs in melee |
| **Archer** | a Bow (keep arrows in the backpack) | attacks hostile mobs at range |
| **Musician** | a Music Box | plays your imported music |

> Pets only fight/work while in **Free Roam** — in Follow and Sit they stay peaceful and won't pick fights.
>
> 🎵 **Note: only Hachiware can currently take the Musician job** — other characters won't play even while holding a music box.

### ❤️ Other interactions
- **Heal** a hurt pet by right-clicking it with food.
- **Open its backpack** by right-clicking with an empty hand (no sneak).

### 🌍 Where they spawn
Pets appear naturally in **Plains, Sunflower Plains, Savanna, Savanna Plateau, Desert, Swamp, and Snowy Plains**. In Creative, use the spawn eggs.

### 🎁 Getting items
- **Weapons** (discipline sticks) are **craftable** — check the recipe book (wool + stick + flint).
- The **Music Box** is **craftable** too (note block + planks + gold ingot).
- **Dolls** and **spawn eggs** are in the Creative inventory. A pet also **drops its doll when it dies**, preserving its backpack and data.

## 🎵 Music Box: play your own songs

1. Open the **Music Box** screen and click **Open Folder** to jump to `config/chiikawa/music`.
2. Drop your `.mp3` / `.wav` files in, hit **Reload**, and pick a track.
3. Give the Music Box to **Hachiware** and set it to the **Musician** job — it'll hold the box and play for you.

Audio is decoded in **pure Java** (mp3spi / JLayer for MP3, the JDK for WAV), encoded with Opus, and streamed from the server to nearby clients — **no external tools needed**.

> Admins can run `/chiikawa music rescan` to force a rescan of the music folder.

## 🗂️ Supported Versions

| Minecraft | Loaders |
| --- | --- |
| 26.1.2 | Fabric · NeoForge |
| 1.21.11 / 1.21.10 / 1.21.8 / 1.21.7 / 1.21.6 / 1.21.5 / 1.21.4 / 1.21.1 | Fabric · NeoForge |
| 1.20.6 | Fabric · NeoForge |
| 1.20.4 / 1.20.2 / 1.20.1 | Fabric · Forge |

Only Minecraft 1.20.1 and newer are maintained.

> Newer snapshot releases (such as the latest 26.x) will be supported once the loader ecosystem stabilizes — please hang tight.

## 🤝 Contributing

Issues and pull requests are welcome.

- Put shared logic in `common`; only loader-specific glue belongs in `fabric` / `neoforge` / `forge`.
- Run `./gradlew :common:test` before opening a PR.
- Generated resources under `**/generated/` are git-ignored and recreated by datagen — don't commit them.
- Each Minecraft version is a separate branch; target the branch your change applies to.

## 📜 License & Credits

- Licensed under [**CC-BY-NC-SA-4.0**](LICENSE).
- Art support by **zoe_1000**.
- *Chiikawa* and its characters are the property of **Nagano**. This is a non-commercial fan project, not affiliated with or endorsed by the rights holders.
