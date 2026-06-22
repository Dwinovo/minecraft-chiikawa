# 跨版本 API 变动参考（Cross-version API notes）

本文件记录在多 Minecraft 版本分支之间移植本 mod 时踩到的 **vanilla / 加载器 API 差异**，按"代际"归纳，方便下次发版直接照表改。

> 维护方式：每个 MC 版本一个分支（分支名 = MC 版本号，如 `1.21.5`）。通常做法是从**最接近的、已发布的旧版本分支** `git cherry-pick` 功能提交，再按本表把屏幕代码翻译到目标版本的 API。
>
> 历史依据：0.0.6-beta 全 13 版本移植（26.1.2 + 1.21.11/10/8/7/6/5/4/1 + 1.20.6/4/2/1）。

---

## 1. 主对照表

把每个 MC 版本归到一个"代际"。同代之间 cherry-pick 通常**零冲突、零改动**。

| 轴 \ 版本 | 26.1.2 | 1.21.11 | 1.21.10 / .8 / .7 / .6 | 1.21.5 / .4 | 1.21.1 | 1.20.6 / .4 / .2 | 1.20.1 |
|---|---|---|---|---|---|---|---|
| **加载器** | fabric + neoforge | fabric + neoforge | fabric + neoforge | fabric + neoforge | fabric + neoforge | fabric + **forge** | fabric + **forge** |
| **资源标识符** | `Identifier` | `Identifier` | `ResourceLocation` | `ResourceLocation` | `ResourceLocation` | `ResourceLocation` | `ResourceLocation` |
| **标识符构造** | `Identifier.fromNamespaceAndPath` | `…fromNamespaceAndPath` | `ResourceLocation.fromNamespaceAndPath` | `…fromNamespaceAndPath` | `…fromNamespaceAndPath` | `new ResourceLocation(ns,path)` | `new ResourceLocation(ns,path)` |
| **Util 包** | `net.minecraft.util.Util` | `net.minecraft.util.Util` | `net.minecraft.Util` | `net.minecraft.Util` | `net.minecraft.Util` | `net.minecraft.Util` | `net.minecraft.Util` |
| **GuiGraphics 类型** | `GuiGraphicsExtractor` ⚠ | `GuiGraphics` | `GuiGraphics` | `GuiGraphics` | `GuiGraphics` | `GuiGraphics` | `GuiGraphics` |
| **Widget 渲染方法** | `renderContents` | `renderContents` | `renderWidget` | `renderWidget` | `renderWidget` | `renderWidget` | `renderWidget` |
| **blit 渲染类型首参** | `RenderPipelines.GUI_TEXTURED` | `RenderPipelines.GUI_TEXTURED` | `RenderPipelines.GUI_TEXTURED` | `RenderType::guiTextured` | 无（直接 `blit(TEX,…)`） | 无 | 无 |
| **Tooltip 机制** | deferred `setTooltipForNextFrame` | deferred | deferred | **立即** `renderTooltip` | 立即 | 立即 | 立即 |
| **Screen 背景模糊** | （extract 体系） | render() 外部处理，无糊 | render() 外部处理，无糊 | ⚠ `Screen.render()` 内联 blur，会糊面板 | 同左结构（无害） | 同左结构（无害） | ⚠ `renderBackground(GuiGraphics)` 单参、render() 不调、无 blur |
| **实体预览 API** | `extractEntityInInventoryFollowsMouse`（矩形） | `renderEntityInInventoryFollowsMouse`（矩形 10 参） | 矩形 10 参 | 矩形 10 参 | 矩形 10 参 | 矩形 10 参 | **点式 7 参** |
| **构建 JDK** | Java 25 | Java 21 | Java 21 | Java 21 | Java 21 | Java 17 / Gradle 8.8 | Java 17 / Gradle 8.8 |

⚠ = 该版本独有的特殊点，移植时最容易翻车，见下方详解。

---

## 2. 各 API 轴详解

### 2.1 blit（贴图绘制）—— 变动最大

签名其余部分一致（`TEXTURE, x, y, (float)u, (float)v, w, h, texW, texH`），只有**渲染类型首参**随代际变化：

```java
// 1.21.6+ / 26.1.2
graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, (float)u, (float)v, w, h, texW, texH);
// 1.21.4 – 1.21.5
graphics.blit(RenderType::guiTextured,      TEXTURE, x, y, (float)u, (float)v, w, h, texW, texH);
// 1.20.1 – 1.21.1（无渲染类型首参）
graphics.blit(                              TEXTURE, x, y, (float)u, (float)v, w, h, texW, texH);
```

- 向更老移植：删 `RenderPipelines.GUI_TEXTURED, ` / `RenderType::guiTextured, `，并删对应 import。
- ⚠ 注意 **9 参带 `texW/texH` 的重载**（`blit(RL, x, y, float u, float v, w, h, texW, texH)`）从 1.20.1 起就存在，非 256×256 图集必须用它；不要退化成 7 参的 `blit(TEX, x, y, u, v, w, h)`（那个默认 256×256）。

### 2.2 Tooltip —— deferred vs 立即

- **1.21.6+ / 26.1.2（deferred）**：`graphics.setTooltipForNextFrame(font, Component, x, y)`；引擎在 `renderWithTooltipAndSubtitles → renderDeferredElements()` 统一 flush。
- **≤1.21.5（立即）**：无 `setTooltipForNextFrame`，用 `graphics.renderTooltip(font, Component, x, y)`，在 `super.render()` 之后当场画。

判定鼠标是否在自定义按钮上，用按钮自身的 `isHovered()`（命中区与可见区 100% 一致），别手写坐标。

### 2.3 ⚠ 容器物品 tooltip —— 所有版本都要手动补（高频 bug）

**1.20.1 ~ 1.21.11 的 `AbstractContainerScreen.render()`/`renderContents` 都不会自动画悬停物品的 tooltip**（老版本会）。每个容器屏幕必须自己 override 并显式调用：

```java
@Override
public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
    super.render(g, mouseX, mouseY, pt);
    this.renderTooltip(g, mouseX, mouseY);   // ← 缺这一刀 = 背包完全没有物品 tooltip
}
```

对照原版任意容器屏幕（`ContainerScreen`、`HopperScreen` 等）都能看到这行。这条曾经因为"deferred 机制看似失效"误诊了很久——真因就是少了这一句。

### 2.4 ⚠ Screen 背景模糊 —— 1.21.4 / 1.21.5 会糊自定义面板

1.21.4 / 1.21.5 的 `Screen.render()` **在开头**调用 `renderBackground() → renderBlurredBackground() → GameRenderer.processBlurEffect()`，这是对**整个帧缓冲的后处理模糊**。若自定义 `Screen` 在 `super.render()` **之前**画了面板，面板会被这次模糊抹糊，而 `super.render()` 内部之后才画的 widget（如列表行）保持清晰 → 现象就是"背景糊、行清晰"。

修法（仅 1.21.4 / 1.21.5 需要）：把面板移到重写的 `renderBackground` 里、在 `super.renderBackground(...)` 之后画；`render()` 改成开头就 `super.render()`：

```java
@Override
public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
    super.renderBackground(g, mx, my, pt);   // 先做世界模糊
    g.blit(/* 面板 */);                       // 模糊之后画 → 不糊
}
@Override
public void render(GuiGraphics g, int mx, int my, float pt) {
    super.render(g, mx, my, pt);             // renderBackground(模糊+面板) + widgets
    /* 文字、tooltip */
}
```

- **1.21.6+**：`renderBackground` 由引擎在 `render()` 外部、之前调用 → 不会糊，面板正常在 `render()` 里画即可。
- **1.20.2 ~ 1.21.1**：`renderBackground(GuiGraphics,int,int,float)` 存在、`Screen.render()` 会调；沿用上面的"面板写进 renderBackground"结构即可（即便不真模糊也无害）。
- ⚠ **1.20.1**：`renderBackground(GuiGraphics)` **只有单参**、且 `Screen.render()` **根本不调它**、无 blur。必须**移除**该 override，面板直接在 `render()` 开头画。

### 2.5 ⚠ 1.20.1 实体预览 API

- **1.20.2+ / 26.1.2**（矩形版，可裁剪到窗口）：
  ```java
  (extract|render)EntityInInventoryFollowsMouse(g, x1, y1, x2, y2, scale, yOffset, mouseX, mouseY, entity);
  ```
- **1.20.1**（仅点式 7 参，无矩形裁剪）：
  ```java
  renderEntityInInventoryFollowsMouse(g, x, y, scale, rotX, rotY, entity);
  // 以展示窗中心为锚，脚部贴近窗口底；rot 用 (centerX-mouseX) / (baseY-mouseY-eyeHeight*scale)
  ```
  无法 1:1 复刻矩形版的裁剪与 yOffset，scale/位置需肉眼微调。

### 2.6 26.1.2 特殊映射（自定义 mappings）

26.1.2 用的是一套 "extract" 渲染管线，与标准 `GuiGraphics` 差别很大：

| 标准（1.21.x） | 26.1.2 |
|---|---|
| `GuiGraphics` | `GuiGraphicsExtractor` |
| `renderBg(g, pt, mx, my)` | `extractBackground(g, mx, my, pt)` |
| `renderLabels(g, mx, my)` | `extractLabels(g, mx, my)` |
| `render(g, mx, my, pt)` | `extractRenderState(g, mx, my, pt)` |
| `graphics.drawString(font, comp, x, y, color, bool)` | `graphics.text(font, comp, x, y, color, bool)` / `centeredText` |
| `renderEntityInInventoryFollowsMouse` | `extractEntityInInventoryFollowsMouse` |
| `imageWidth/imageHeight` 可赋值 | **FINAL**，用 5 参父构造 `super(menu, inv, title, w, h)` |

其余（`Identifier`、`net.minecraft.util.Util`、`RenderPipelines`、deferred tooltip、`renderContents`）与 1.21.11 一致。

---

## 3. 移植流程（cherry-pick 工作流）

1. `git checkout <目标版本分支>`，`git fetch origin <分支>`（若曾删过本地引用）。
2. **先普查目标版本的代际**：看 baseline 屏幕用的 blit/构造器/Util，再 `javap` vanilla jar 确认 tooltip / Screen.render / blit 重载 / 实体预览签名。jar 路径：
   `~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged/<ver>-*/…merged-<ver>-*.jar`（取非 `intermediary` 那个）。
3. 从**最接近的已发布旧分支** cherry-pick 0.0.5→0.0.6 的功能提交（`git log --oneline --reverse v0.0.5-<src>-beta..<src>`），**跳过调试/被取代的提交**。
4. 屏幕文件冲突时，直接 `git checkout v0.0.6-<src>-beta -- <screen>` 取源分支最终版，再按本表 `sed` 翻译该版本的 API 差异。
5. `./gradlew :common:compileJava :fabric:compileJava :<neoforge|forge>:compileJava`（toolchain 会自动选 JDK）。`forge` 模块在 1.20.x 分支，`neoforge` 在 1.21.x+。
6. 把 `.mcp.json` 加进 `.gitignore`（本地 MCP 配置，含机器绝对路径，不入库）。
7. **本地提交，先不推送/打 tag**；有真实 API 改动或视觉变化时让作者验证。
8. 验证通过后 `git push origin <分支>` + `git tag -a v0.0.6-<ver>-beta` + 推 tag。`publish.yml` 监听 `v*`，自动识别 forge/neoforge 并发到 Modrinth / CurseForge / GitHub Releases。

### 代际边界速记
- **1.21.5 → 1.21.6**：最大断层。`RenderType::guiTextured`→`RenderPipelines`、新增 deferred tooltip、`Screen.render()` 不再内联 blur。
- **1.21.10 → 1.21.11**：`ResourceLocation`→`Identifier`、`net.minecraft.Util`→`net.minecraft.util.Util`、`renderWidget`→`renderContents`。
- **1.21.1 → 1.20.6**：`fromNamespaceAndPath`→`new ResourceLocation`、neoforge→forge、JDK 21→17。
- **1.20.2 → 1.20.1**：`renderBackground` 单参无 blur、实体预览退化为点式 7 参。

---

## 4. 方块交互 / 农夫职业 API（farming）

农夫职业（种植/收割）用到的 vanilla 方块交互 API，移植时注意以下几点。多数是**跨版本稳定**的原版方法，只有少量在 26.1.2 自定义 mappings 下与 Forge 习惯不同。

### 4.1 ⚠ `BlockPlaceContext`「无玩家」构造器在 vanilla 是 protected

要让宠物像玩家一样原生放置方块（`BlockItem.place`），需要一个 player 为 null 的 `BlockPlaceContext`。**Forge 把 5 参构造器开放成了 public**（所以 TLM 能直接 `new BlockPlaceContext(level, null, hand, stack, hit)`），但 **vanilla / 26.1.2 该构造器是 `protected`**，直接 new 报错。解法是匿名子类绕过：

```java
BlockHitResult hit = new BlockHitResult(hitVec, Direction.UP, basePos, false);
// 匿名子类即可访问 protected 构造器；无玩家放置
BlockPlaceContext ctx = new BlockPlaceContext(level, null, InteractionHand.MAIN_HAND, seed, hit) {};
boolean placed = ((BlockItem) seed.getItem()).place(ctx).consumesAction();
```

`place()` 成功时会自动 `shrink` 种子堆，不用手动扣。

### 4.2 26.1.2 类/方法重命名

| 标准（1.21.x / Forge 习惯） | 26.1.2 |
|---|---|
| `FarmBlock` | **`FarmlandBlock`**（`fallOn` / `turnToDirt` 仍在） |
| `Block.getDrops(..., ItemStack tool)` | 末参类型是 **`ItemInstance`**（`ItemStack implements ItemInstance`，直接传 stack 即可） |
| `ItemNameBlockItem`（小麦种子等的类） | **已移除**——种子物品现在就是普通 `BlockItem`。判定种子用 `item instanceof BlockItem && getBlock() instanceof CropBlock/StemBlock`，**不要**引用 `ItemNameBlockItem` |

### 4.3 防踩耕地：本 mod 无需处理

TLM 用 Forge `FarmlandTrampleEvent` 取消女仆踩踏。本 mod **不需要任何代码**：`FarmlandBlock.fallOn` 的踩踏条件是 `getBbWidth()² × getBbHeight() > 0.512F`，而宠物体型 `0.6 × 0.8` → `0.6²×0.8 = 0.288 < 0.512`，**天然不会踩坏耕地**。若将来把宠物体型放大到超过阈值，才需要 mixin 进 `FarmlandBlock.fallOn`（本 mod 目前无 mixin 基建，两个加载器都要新配）跳过 `AbstractPet`。

### 4.4 收割 = 原生掉落进背包，不撒地上

`AbstractPet.dropResourcesToPetInv` 用 `Block.getDrops(state, serverLevel, pos, be, pet, tool)` 带工具算掉落（Fortune 生效），`SimpleContainer.addItem` 返回的余量再 `Block.popResource` 落地，最后 `state.spawnAfterBreak(...)`。成熟 `CropBlock` 收割后 `setBlock(crop.defaultBlockState())` 即 age 归零原地补种（**省去 TLM 那个读 `AGE` 属性的 mixin**）。以上方法全是原版、跨版本稳定。

### 4.5 农夫可扩展架构（对标 TLM `IFarmTask`/`ISpecialCropHandler`）

`entity/brain/task/farmer/crop/` 下：`CropHandler` 接口 + `DefaultCropHandler`（标准作物）+ `FarmRegistry`（`Map<Item/Block, CropHandler>`，未注册回落默认）。加异类作物 = 写一个 `extends DefaultCropHandler` 只改差异点，再 `FarmRegistry.register(seed, crop, handler)`（见 `NetherWartCropHandler`）。注册在**两个加载器入口各调一次** `FarmRegistry.init()`（neoforge 入口不走 `CommonClass.init()`，别只加一处）。

### 4.6 农夫 sensor 性能模式（移植时照搬）

宠物 AI 找方块**不要**在扫描里逐格寻路——`PathNavigation.createPath` 是单次最贵的操作，一次寻路 ≈ 甚至 >> 整片邻域的 `getBlockState` 平铺扫描。本 mod 的 `PetFarmerWorkSensor` 用四条原则把农夫 AI 从"每秒几十~上百次 A\*"降到"偶尔 1 次 A\*"：

1. **扫描不寻路**：sensor 只用廉价方块判定选候选;可达性那次 A\* 移到 WalkTo 行为里**对最终目标只做一次**。
2. **拉黑表退避**：WalkTo 发现目标够不到 → `AbstractPet.blacklistUnreachable(pos)`(瞬态、不存档、10s 过期),下次扫描跳过,自然收敛到可达目标。
3. **合并 + 满足即跳过**：原来 plant/harvest/container 三个 sensor 各扫一遍 → 合成一个 `PetFarmerWorkSensor` 一次螺旋同时收三类;且所有目标都已就位时整个扫描直接 return(连廉价遍历都省)。复验已记忆目标只用 `getBlockState`,**绝不重新寻路**。
4. **自适应退避**：连续空搜逐级拉长搜索间隔(每级 +30t,上限 5 级 ≈ ~160t),移动到新位置或找到活立即清零恢复快搜。退避状态放 sensor 实例字段即可——**每个实体的 brain 各自持有一份 sensor 实例**(`SensorType` 工厂每 brain 造一个),所以实例字段天然是 per-pet。

⚠ **不要**用 vanilla POI(`PoiManager`)索引箱子等常见方块:POI 适合稀疏稳定的特殊方块,索引常见方块会在全服每个区块产生内存/处理开销,是净负优化。容器的"缓存"用每只宠物的 `CONTAINER_POS` 记忆即可。

`Sensor()` 无参默认 `DEFAULT_SCAN_RATE = 20`(每 20t 一扫,带随机抖动),自定义 sensor 不写构造器就是这个节流频率,别误以为每 tick 都在跑。
