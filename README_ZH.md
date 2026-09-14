<div align="center">

<img src="https://raw.githubusercontent.com/Dwinovo/minecraft-chiikawa/1.21.1/common/src/main/resources/logo.png" alt="Chiikawa Mod" width="320" />

# Chiikawa Mod

**把《ちいかわ》的世界搬进 Minecraft —— 可驯服的小可爱、会自己干活的职业系统,还有一台能播放你自己歌曲的音乐盒。**

[English](README.md) · **简体中文**

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1%20–%2026.1.2-62B47A?logo=minecraft&logoColor=white)](#支持的版本)
[![Loaders](https://img.shields.io/badge/Loaders-Fabric%20%7C%20NeoForge%20%7C%20Forge-blue)](#支持的版本)
[![Modrinth](https://img.shields.io/badge/Download-Modrinth-00AF5C?logo=modrinth&logoColor=white)](https://modrinth.com/mod/pT971QUb)
[![CurseForge](https://img.shields.io/badge/Download-CurseForge-F16436?logo=curseforge&logoColor=white)](https://www.curseforge.com/minecraft/mc-mods/chiikawa)
[![License](https://img.shields.io/badge/License-CC--BY--NC--SA--4.0-lightgrey)](LICENSE)
[![GitHub Stars](https://img.shields.io/github/stars/Dwinovo/minecraft-chiikawa?style=flat&logo=github&label=Star)](https://github.com/Dwinovo/minecraft-chiikawa)

</div>

---

> [!IMPORTANT]
> ⭐ **喜欢这个模组?[点个 Star](https://github.com/Dwinovo/minecraft-chiikawa)** —— 只要一下,是最直接的支持。
> 🐛 **遇到 Bug 或有想法?[来提 Issue](https://github.com/Dwinovo/minecraft-chiikawa/issues/new/choose)** —— 请用 GitHub Issues,不要发在 Modrinth / CurseForge 评论区,这样才不会被淹没。

> ちいかわ、八割、兔兔……这些治愈又有点搞笑的小家伙,现在会住进你的世界。
> 驯服它们、给它们一把工具,它们就会替你种田、战斗、甚至**抱着音乐盒为你弹一首你最爱的歌**。
> 它们会卖萌、会受伤、会发出招牌怪叫——而当它们倒下时,只要一块蛋糕,就能把它们带回来。 🎂

<!--
  📸 画廊占位区 —— 强烈建议在这里放 2~4 张截图 / GIF,这是决定别人点不点下载的关键:
  - 一群小可爱跟着玩家跑(展示模型 + 动画)
  - 农夫在田里种地收割
  - 八割抱着音乐盒弹琴(配字幕「演奏中 ♪」)
  - 把玩偶放上蛋糕复活的瞬间
  示例写法:
  <div align="center">
    <img src="docs/img/farming.gif"  width="45%" />
    <img src="docs/img/music.gif"    width="45%" />
  </div>
-->

## ✨ 为什么你会想装它

- 🐾 **七只原作角色**,全部可驯服 —— Chiikawa(吉伊)、Hachiware(八割)、Usagi(兔兔)、Shisa(狮酱)、Momonga(松鼠)、Kurimanju(栗子馒头)、Rakko(海獭),每只都有独立模型、贴图与性格。
- 🧰 **会自己干活的职业系统** —— 把工具丢进它的背包,切到「工作」模式,它就开始替你忙活:**农夫**种田收割、**剑士**近战御敌、**弓手**远程压制、**乐师**为你演奏。
- 🎵 **能播放你自己歌曲的音乐盒** —— 把 `.mp3` / `.wav` 丢进文件夹,游戏里就能听。纯 Java 解码,**不需要任何外部 ffmpeg**;多人服里附近的玩家会一起听到同一首歌。
- 🔊 **角色专属音效** —— 兔兔的招牌怪叫、各角色的驯服/受伤声,让它们「活」起来。
- 🎒 **宠物背包** —— 空手右键打开它的物品栏,管理工具和随身物。
- 🪆 **玩偶 & 刷怪蛋** —— 每只角色都有专属玩偶(试试把玩偶放在蛋糕上 🎂),创造模式里还能直接刷出来。
- ⚔️ **角色武器** —— 可合成的招牌「惩戒棒」,吉伊 / 八割 / 兔兔各一把。

## 📥 下载

| 平台 | 链接 |
| --- | --- |
| **Modrinth** | https://modrinth.com/mod/pT971QUb |
| **CurseForge** | https://www.curseforge.com/minecraft/mc-mods/chiikawa |

> **Fabric** 玩家还需要安装 [Fabric API](https://modrinth.com/mod/fabric-api)。

## 🎮 快速上手

新手看这一段就够了 —— 大多数「怎么不动啊」的反馈,都是漏了下面某一步。

### 🍖 驯服
手持**几乎任何食物**(苹果、面包、曲奇、胡萝卜、土豆、熟肉、西瓜片、甜浆果、金苹果……),**右键点击小可爱**。

> ⚠️ **驯服是概率制的。** 每次喂食有一定几率成功,所以多喂几次!出现**爱心**=驯服成功;冒**烟/一脸困惑**=还没成功,再来。花已经不管用了,请用食物。

### 🧭 三种模式 —— 跟随 / 坐下 / 工作
驯服后,**潜行(Shift)+ 空手右键**循环切换:

- **跟随** —— 小可爱跟着你跑。
- **坐下** —— 待在原地不动。
- **工作** —— 在你设定的位置附近开始执行它的职业。

### 💼 职业
一只宠物干什么活,**由它手持的工具决定**。空手右键打开背包,把工具放进去,再切到「工作」模式:

| 职业 | 给它…… | 它会做什么 |
| --- | --- | --- |
| **农夫 Farmer** | 一把锄头 | 自动种植并收割附近的作物(支持模组种子) |
| **剑士 Fencer** | 一把剑(或角色的惩戒棒) | 近战攻击附近的敌对生物 |
| **弓手 Archer** | 一把弓(背包里要有箭) | 远程射击敌对生物 |
| **乐师 Musician** | 一台音乐盒 | 演奏你导入的音乐 |

> 宠物只在「工作」模式下战斗/干活;跟随模式下它们乖乖的、不会主动出击。
>
> 🎵 **注意:乐师职业目前只有「八割 Hachiware」能胜任** —— 其他角色拿着音乐盒也不会演奏。

### ❤️ 其他互动
- **治疗**:对受伤的宠物右键喂食即可回血。
- **打开背包**:空手右键(不潜行)。

### 🌍 它们在哪出现
小可爱会自然生成在 **平原、向日葵平原、热带草原、热带高原、沙漠、沼泽、积雪平原**。创造模式里直接用刷怪蛋。

### 🎁 怎么获得物品
- **武器**(惩戒棒)可**合成** —— 翻翻配方书(羊毛 + 木棍 + 燧石)。
- **音乐盒**也可**合成**(音符盒 + 木板 + 金锭)。
- **玩偶**、**刷怪蛋**在创造物品栏里。宠物**死亡时会掉落自己的玩偶**,并保留它的背包和数据。

## 🎵 音乐盒:放你自己的歌

1. 打开**音乐盒**界面,点 **Open Folder(打开文件夹)**,会跳到 `config/chiikawa/music`。
2. 把你的 `.mp3` / `.wav` 丢进去,点 **Reload(重载)**,选一首曲子。
3. 把音乐盒交给**八割**,切到**乐师**职业,它就会抱着音乐盒为你演奏。

音频在**纯 Java** 里完成解码(MP3 用 mp3spi / JLayer,WAV 用 JDK 自带),再用 Opus 编码,由服务端流式推送给附近的客户端 —— **全程不需要任何外部工具**。

> 管理员可用 `/chiikawa music rescan` 强制重新扫描音乐目录。

## 🗂️ 支持的版本

| Minecraft | 加载器 |
| --- | --- |
| 26.1.2 | Fabric · NeoForge |
| 1.21.11 / 1.21.10 / 1.21.8 / 1.21.7 / 1.21.6 / 1.21.5 / 1.21.4 / 1.21.1 | Fabric · NeoForge |
| 1.20.6 | Fabric · NeoForge |
| 1.20.4 / 1.20.2 / 1.20.1 | Fabric · Forge |

仅维护 Minecraft 1.20.1 及更新版本。

> 更新的快照版本(如最新发布的 26.x)会在加载器生态稳定后陆续跟进 —— 请稍候。

## 🤝 参与贡献

欢迎 Issue 与 PR。

- 共享逻辑放 `common`;只有加载器特有的胶水代码才放进 `fabric` / `neoforge` / `forge`。
- 提 PR 前先跑 `./gradlew :common:test`。
- `**/generated/` 下的生成资源已被 git 忽略,由 datagen 重建,**不要提交**。
- 每个 Minecraft 版本是独立分支;请把改动提到对应的分支上。

## 📜 许可与致谢

- 基于 [**CC-BY-NC-SA-4.0**](LICENSE) 协议开源。
- 美术支持:**zoe_1000**。
- 《ちいかわ》及其角色版权归 **ナガノ(Nagano)** 所有。本项目为非商业同人作品,与版权方无任何关联,亦未获其背书。
