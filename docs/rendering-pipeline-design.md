# 自研渲染与动画管线设计

本文档描述 Chiikawa 模组脱离 Geckolib 后的自研 Bedrock 模型渲染与动画管线。架构采用 GeckoLib 的多 controller 模型（state-driven handler + per-controller blend mode），但保留我们自己的纯函数采样器、扁平 pose buffer 和 BakeStamp 重载安全机制。

## 设计目标

1. 稳定支持 Minecraft 1.21.6 到 26.1.2 等多分支迁移。
2. 不依赖 Geckolib 的实体动画计时和控制器状态。
3. 消除 GUI 预览双 `extractRenderState` 导致的动画倍速和抖动。
4. 保留 Blockbench Bedrock Entity 工作流，方便动画师交付 `.geo.json` 和 `.animation.json`。
5. 把渲染层、动画采样层、宠物业务状态层解耦。
6. 用统一的 controller 抽象覆盖"基础循环 + 一次性触发 + 装饰循环"三类需求，避免双系统并存。

## 总体结构

代码位于 `common/src/main/java/com/dwinovo/chiikawa/anim`，分为九个职责层：

```text
format → compile → molang → baked → controller → runtime → state → render → api
```

| 层 | 主要职责 | 代表类 |
|---|---|---|
| `format` | 承接 Bedrock JSON 反序列化 | `BedrockGeoFile` |
| `compile` | 资源重载时把 JSON 烘焙成运行时数据 | `BedrockResourceLoader`, `ModelBaker`, `AnimationBaker` |
| `molang` | 编译和执行有限范围的 Molang 表达式 | `MolangCompiler`, `MolangContext`, `MolangNode` |
| `baked` | 不可变共享模型和动画数据 | `BakedModel`, `BakedAnimation`, `BakedBoneChannel`, `LoopMode`, `BakeStamp` |
| `controller` | GeckoLib 风格的 controller 配置和实例 | `BlendMode`, `ControllerHandler`, `ControllerConfig`, `ControllerInstance`, `ControllerSnapshot` |
| `runtime` | 每实体的动画时间线和采样函数 | `PetAnimator`, `AnimationChannel`, `PoseSampler`, `PoseMixer` |
| `state` | 把游戏状态映射为候选动画名 | `PetAnimContext`, `PetAnimationResolver` |
| `render` | Minecraft 客户端集成 | `ChiikawaEntityRenderer`, `ChiikawaRenderState`, `ModelRenderer` |
| `api` | 业务侧接口和注册表 | `ChiikawaAnimated`, `ModelLibrary`, `AnimationLibrary` |

原则：左侧层不依赖右侧层。`format`、`compile` 只在资源加载时运行；`baked` 只读共享；`controller` 是配置类型；`runtime` 是每个实体一份；`state` 是纯决策；`render` 负责与 Minecraft 渲染 API 对接。

## 资源加载流程

资源路径约定：

```text
common/src/main/resources/assets/<namespace>/
├── models/entity/<pet_name>.json
├── animations/<pet_name>.json
└── textures/entities/<pet_name>.png
```

`<pet_name>` 必须和实体注册名 path 一致，例如实体 `chiikawa:chiikawa` 对应：

```text
models/entity/chiikawa.json
animations/chiikawa.json
textures/entities/chiikawa.png
```

加载链路：

```text
ResourceManager reload
    -> BedrockResourceLoader.onResourceManagerReload
    -> BakeStamp.next()                          ← 代际 +1
    -> Gson/JsonParser
    -> ModelBaker / AnimationBaker / MolangCompiler  （都打上当前 stamp）
    -> ModelLibrary.replaceAll / AnimationLibrary.replaceAll
```

烘焙后的注册表 key：

| 数据 | key 形式 |
|---|---|
| 模型 | `<namespace>:<pet_name>` |
| 动画 | `<namespace>:<pet_name>/<animation_name>` |
| 贴图 | `<namespace>:textures/entities/<pet_name>.png` |

业务层只需要产出动画名，例如 `idle`、`harvest`、`scratch_head`，renderer 会用当前模型 key 拼出完整动画 key。

## Baked 数据模型

`BakedModel` 是模型的不可变运行时表示：

| 字段 | 含义 |
|---|---|
| `bones` | 骨骼数组，包含 parent、pivot、rest rotation、cube range |
| `cubes` | cube 顶点和 UV 数据 |
| `rootBones` | 根骨骼索引 |
| `boneIndex` | 骨骼名到索引的查找表 |
| `textureWidth/textureHeight` | Bedrock 贴图尺寸 |
| `bakeStamp` | 该模型烘焙时的 `BakeStamp` 代际值 |

`BakedAnimation` 是动画的不可变运行时表示：

| 字段 | 含义 |
|---|---|
| `name` | 动画短名 |
| `durationSec` | 动画时长，秒 |
| `loopMode` | `PLAY_ONCE` / `LOOP` / `HOLD_ON_LAST_FRAME` 三态 |
| `channels` | 扁平化后的骨骼 channel |
| `bakeStamp` | 该动画烘焙时的代际值（与 BakedModel 一致） |

`BakedBoneChannel` 会把同一根骨骼的 rotation、position、scale 拆成独立 channel。每个 channel 已经完成骨骼名解析、时间轴整理、插值模式记录和 Molang slot 编译。

`bakeStamp` 让 controller 可以在资源重载后检测到自己持有的旧代引用并主动清空（详见下面的 controller 章节）。

## 坐标与镜像约定

Blockbench 的 Bedrock Entity 导出存在 X 轴镜像约定。管线选择在 bake 阶段一次性对消镜像，而不是在 renderer 里临时翻转。

处理规则：

| 数据 | bake 阶段处理 |
|---|---|
| bone pivot X | 取反 |
| cube origin X | 转换为 `-(originX + sizeX)` |
| cube pivot X | 取反 |
| bone/cube rotation X、Y | 取反 |
| rotation Z | 保持 |
| animation rotation X、Y | 取反 |
| animation position X | 取反 |
| animation scale | 保持 |
| cube UV | 对非 mirror cube 做 U 翻转补偿 |

renderer 因此只做两件事：

```text
rotateY(180 - bodyRot)
scale(1 / 16)
```

模型顶点和骨骼 pivot 保持 Bedrock 的 pixel 单位。PoseStack 进入模型渲染时已经被缩放到块空间。

## Pose Buffer

每帧为每个实体分配一个 pose buffer：

```text
float[boneCount * 9]
```

每根骨骼 9 个 slot：

| slot | 含义 | identity |
|---|---|---|
| `0..2` | rotation delta，XYZ Euler，弧度 | `0, 0, 0` |
| `3..5` | position offset，pixel 单位 | `0, 0, 0` |
| `6..8` | scale multiplier | `1, 1, 1` |

每帧流程：

```text
PoseSampler.resetIdentity                   ← 清回 identity
    -> for each ControllerSnapshot：        ← 按注册序写入 poseBuf
         按 BlendMode 调 PoseSampler.sample
    -> for each BoneInterceptor stage：     ← 程序化覆写
         interceptor.apply(...)
    -> ModelRenderer.render                 ← 骨骼 DAG 遍历 + 顶点发射
    -> RenderLayer 注册表（HeldItemLayer 等）
```

后采样的 controller 在 `OVERRIDE` 模式下覆盖前者，在 `ADDITIVE` 模式下叠加。`BoneInterceptor` 在所有 controller 之后跑，是终极覆盖（head look-at 永远赢）。

## 纯函数采样

旧动画库的问题来自可变累计时间。我们的采样规则是：

```java
PoseSampler.sample(channel, blendMode, nowNs, ctx, poseBuf)
```

`AnimationChannel` 只记录：

```java
record AnimationChannel(BakedAnimation animation, long startTimeNs, boolean looping)
```

采样时通过 `nowNs - startTimeNs` 计算本地动画时间。相同输入得到相同输出，没有 `lastAnimatableAge` 之类的可变累计字段。

这个约定直接解决了 GUI 预览实体时的双 extract 问题：

1. `extractRenderState` 可以被同一帧调用多次。
2. controller 的 handler 被设计成幂等的（同一个 `BakedAnimation` 不重启计时）。
3. 采样时间来自 `System.nanoTime()` 和 channel 起点。
4. 重复 extract 不会让动画多走一遍。

## Controller 模型

`PetAnimator` 不再是固定槽位，而是 **registration order 排好的 controller 列表**。每个 `ControllerConfig` 描述一条独立的播放轨道：

| 字段 | 含义 |
|---|---|
| `name` | 标识符；外部 `playOnce(name, anim)` 用这个寻址 |
| `blendMode` | `OVERRIDE` 或 `ADDITIVE` |
| `transitionSec` | 该 controller 切换动画时的 crossfade 长度，0 = 无 fade |
| `handler` | `(state, ctx) -> BakedAnimation?` 纯函数 |

每个 controller 在每只宠物身上对应一个 `ControllerInstance`（mutable per-entity 状态），持有：

- 当前 / 前一个 `AnimationChannel`（用于 crossfade）
- triggered 一次性动画（playOnce 设进来的，覆盖 handler 直到结束）
- 进入 fade 的起始时间和时长

### 默认注册的 controllers

`ChiikawaEntityRenderer` 基类构造函数自动注册：

| name | blendMode | transition | 用途 |
|---|---|---|---|
| `main` | OVERRIDE | 0.16s | 状态驱动基础循环（idle/run/sit/work_idle_*） |
| `action` | OVERRIDE | 0.15s | 接收 `playOnce` 的一次性动作（harvest/slash/...） |
| `reaction` | OVERRIDE | 0.15s | 接收 `playOnce` 的一次性反应（happy/hurt/...） |

`action` / `reaction` 的 `transitionSec` 用于触发动画结束时的**自动 fade-out**——动画师不需要在 harvest 末尾手动做"回到中性手臂"的关键帧；trigger 播完时 controller 在 0.15 秒内把贡献淡出，骨头平滑过渡回 `main` 的 base loop 写入的姿态。fade-IN 仍是即时（gameplay 事件应该立刻响应）。

子类可以追加装饰性 controllers：

```java
public ChiikawaRenderer(EntityRendererProvider.Context ctx) {
    super(ctx, "chiikawa");
    addLoopingController("blink",  BlendMode.OVERRIDE, "blink");
    addLoopingController("breath", BlendMode.ADDITIVE, "breath");
}
```

`addLoopingController(name, blendMode, animationName)` 是语法糖：注册一个 `transitionSec=0`、handler 永远返回指定动画的 controller。

### Blend mode 数学

`PoseSampler` 在 ADDITIVE 模式下按 channel 类型分别合成（对齐 GeckoLib `AnimationProcessor` 的语义）：

| 通道 | OVERRIDE | ADDITIVE |
|---|---|---|
| rotation | `pose[i] = animValue` | `pose[i] += animValue` |
| position | `pose[i] = animValue` | `pose[i] += animValue` |
| scale | `pose[i] = animValue` | `pose[i] *= animValue` |

scale 必须是乘法因为 identity 是 `1.0`。两个 ADDITIVE controller 同时贡献 scale 时，朴素 `+=` 会把 1 + 1 = 2 累成 4 倍模型。

动画文件**没 keyframe 的通道完全不动 poseBuf**，无论什么 blend mode。这意味着动画师只在他们想贡献的通道上 keyframe，自然就拿到了想要的合成结果——不需要专门的"仅旋转加性"细分模式。

### 一次性触发（playOnce）

`ControllerInstance.playOnce(animation, nowNs)` 把外部传入的动画塞进 controller，**优先级高于 handler**。在动画的 baked 时长内，handler 被跳过，sample 该 controller 出的就是 triggered animation。时长结束后自动清空，handler 恢复决策。

`LoopMode.HOLD_ON_LAST_FRAME` 触发的动画不自动清，直到下一次 `playOnce` 替换或 `clearTrigger()` 显式清掉。

这是 [`AbstractPet`](../common/src/main/java/com/dwinovo/chiikawa/entity/AbstractPet.java) 同步触发包的接收端：

```text
server task
    -> pet.triggerAction(PetAction.HARVEST)
    -> entityData.set(packed)            ← seq +1，networkId 写低 8 位
    -> 客户端 onSyncedDataUpdated
    -> 解析 networkId 找候选动画
    -> animator.playOnce("action", anim)
```

`reaction` 走平行的 `REACTION_TRIGGER` 字段和 `playOnce("reaction", anim)`。

## 渲染流程

`ChiikawaEntityRenderer.extractRenderState`：

1. 从 `LivingEntity` 读取 body rotation、head rotation、pitch、scale、walkSpeed。
2. 把 head_yaw / head_pitch snapshot 到 state 自己的字段（避开 InventoryScreen 后续覆写）。
3. 保存主手物品 `ItemStack`。
4. `animator.ensureInitialised(controllerConfigs)` 首次构建每实体的 `ControllerInstance` 列表。
5. `animator.setPhaseSeed(uuid)` 第一帧锁存相位偏移（让多只宠物的装饰循环错开）。
6. `animator.clearStale(currentModel.bakeStamp)` 清掉资源重载残留的旧代引用。
7. `animator.tick(state, ctx, nowNs)` —— 每个 controller 自行决策当前动画。
8. `state.controllerSnapshots = animator.snapshot()` 把每个 controller 的当前状态打成不可变快照数组。

`submit`：

1. 获取 `BakedModel`。
2. 创建并初始化 pose buffer。
3. 填 MolangContext（`ground_speed` 等）。
4. 遍历 `controllerSnapshots`，按各自 `BlendMode` 调 `PoseSampler.sample` 写入 poseBuf；OVERRIDE controller 处于 fade 中时双缓冲采样 + `PoseMixer.blend`。
5. 按 stage 顺序运行 `BoneInterceptor`（LOOK_AT → PHYSICS_SECONDARY → OCCLUSION）。
6. 渲染模型 mesh。
7. 各 RenderLayer.submit（HeldItemLayer 把主手物品挂到 `RightHandLocator`）。

## 过渡机制：两种 fade，全部由代码自动处理

每个 controller 有一个 `transitionSec`，同时控制两类 fade。两类 fade 不会同时发生（进入其一会取消另一类）。

**1. 单 controller 内部切换**（previous + current 都活着）

handler 在 idle → sit 之间切换时：

```text
ControllerInstance.switchTo(...)
    -> 把当前 channel 转成 previous
    -> 记录 fadeStartNs = nowNs（必须用 nowNs，不能用动画的 startNs，详见下面）
    -> fadeDurationSec = config.transitionSec()

submit / sampleController.crossfade(...)
    -> fromPose = poseBuf.clone(), toPose = poseBuf.clone()
    -> PoseSampler.sample(previous, mode, ..., fromPose)
    -> PoseSampler.sample(current,  mode, ..., toPose)
    -> PoseMixer.blend(fromPose, toPose, smoothstep(alpha)) → poseBuf
```

OVERRIDE 和 ADDITIVE 走同一条路径——ADDITIVE 的"采样"是 `+=`/`*=`，做完后 temp 缓冲里的就是"叠加完的姿态"，再 lerp 也是有效的。

**2. controller 整体停止**（`fadingOut = true`）

handler 返回 `null`，或 `playOnce` 触发的动画播完时：

```text
ControllerInstance.enterStopFade(nowNs)
    -> previousAnim = null（取消任何 pending 切换）
    -> fadeStartNs = nowNs
    -> fadeDurationSec = config.transitionSec()
    -> fadingOut = true

submit / sampleController.stopFade(...)
    -> existing = poseBuf.clone()      ← 下层 controller 已写入的姿态
    -> PoseSampler.sample(current, mode, ..., poseBuf)   ← 完整贡献
    -> PoseMixer.blend(poseBuf, existing, alpha, poseBuf)
       — alpha 0 = 完整贡献保留；alpha 1 = 完全消退到 existing
```

这就是**自动跨 controller 过渡**：`action` controller 的 harvest 播完后，会在 0.15 秒内从 harvest 末态平滑过渡到 `main` 当前的 idle/sit/run。动画师不用在动画末尾做收尾关键帧。

`PoseMixer` 对 position/scale 做线性插值，对 rotation 做最短路径角度插值，避免 `179° -> -179°` 这类切换绕一大圈。

**关键不变量**：`fadeStartNs` 必须等于 `nowNs`，**绝不能等于新动画的 `startNs`**。对循环动画 `startNs = phaseSeed`（启动时锁定的"过去时间 - UUID 偏移"），用错会让 fade alpha 在第一帧 clamp 到 1.0，整个 fade 系统瞬间退化为硬切。回归测试见 [`ControllerInstanceTest.withinControllerFadeUsesNowNsNotPhaseSeed`](../common/src/test/java/com/dwinovo/chiikawa/anim/controller/ControllerInstanceTest.java)。

## 程序化骨骼干预

`BoneInterceptor` 是动画采样之后的程序化覆写点，按 `Stage`（`LOOK_AT` / `PHYSICS_SECONDARY` / `OCCLUSION`）分阶段运行。当前默认拆成两个互不干扰的 interceptor：

- `HeadLookInterceptor`（`Stage.LOOK_AT`）—— 头部 yaw/pitch 跟随玩家
- `IdleSwayInterceptor`（`Stage.PHYSICS_SECONDARY`）—— 耳朵摆动 + 尾巴摇

它们覆盖以下骨骼：

| 骨骼 | 用途 |
|---|---|
| `AllHead` | look-at/head pitch/head yaw |
| `LeftEar` / `RightEar` | 闲置摆动 |
| `tail` | 闲置摆动 |

设计意图是让"过渡、注视、微动"走程序化管线，而不是让动画师为每个状态都做大量过渡片段。动作师主要交付稳定可复用的基础循环、行为动作和情绪反应。

interceptor 在所有 controller 之后跑，因此也是终极覆盖——头部跟随玩家这件事不会被任何动画文件改写。

## 物品挂载

手持物挂载在骨骼：

```text
RightHandLocator
```

`HeldItemLayer`（注册在 `ChiikawaEntityRenderer.renderLayers` 列表里）会调用通用的 `BoneTransformWalker` 沿骨骼父链应用 rest transform 和 pose transform，最终把 item renderer 提交到该 locator。

新增挂件（道具、披风、发光眼睛等）只需实现 `RenderLayer` 接口，由 renderer 子类在构造函数里 `addRenderLayer(...)` 注册，无需改主路径。

注意：模型 PoseStack 已经处于 `1/16` pixel 缩放后空间，但 Minecraft item renderer 使用块单位。`HeldItemLayer` 在提交物品前会恢复到块单位，避免物品尺寸异常。

## 与状态机的边界

渲染管线不直接理解宠物 AI 和任务。它只消费来自 `PetAnimContext` 的快照：

```text
PetAnimContext
    -> "main" controller 的 handler
    -> PetAnimationResolver.resolve(ctx) → List<String> 候选名
    -> AnimationLibrary lookup → BakedAnimation
    -> ControllerInstance.tick → 写进 currentAnim
    -> snapshot → renderer 采样
```

新增任务动作时，优先改状态机枚举（`PetAction` / `PetReaction`）和触发点，而不是在 renderer 里写业务判断。

## 动画资源命名契约

基础循环：

| 名称 | 用途 |
|---|---|
| `idle` | 默认站立 |
| `run` | 移动 |
| `walk` | 预留，未来有慢走循环时启用 |
| `sit` | 坐下 |
| `work_idle_farmer` | 农夫工作待机 |
| `work_idle_fencer` | 剑士工作待机 |
| `work_idle_archer` | 弓手工作待机 |

行为动作（由 `playOnce("action", ...)` 触发）：

| 名称 | fallback |
|---|---|
| `pickup` | `use_mainhand` |
| `harvest` | `use_mainhand` |
| `plant` | `use_mainhand` |
| `deposit` | `use_mainhand` |
| `slash` | `use_mainhand` |
| `bow_draw` | `sword_attack` |
| `bow_release` | `sword_attack` |

情绪反应（由 `playOnce("reaction", ...)` 触发）：

| 名称 | fallback |
|---|---|
| `happy` | 无 |
| `hurt` | 无 |
| `scratch_head` | `confused` |
| `revive` | `happy` |

装饰循环（由 renderer 子类的 `addLoopingController` 启用）：

| 名称 | 推荐 blend mode | 仅 keyframe 的骨骼 |
|---|---|---|
| `blink` | OVERRIDE | 眼皮 |
| `breath` | ADDITIVE | 胸腹 rotation |
| `tail_idle` | ADDITIVE | 尾骨 rotation |

fallback 让旧资源能够继续工作。新增动画时优先用语义名称，旧名只作为兼容层保留。

## 扩展规则

新增动画能力时优先按以下顺序扩展：

1. 新增语义状态或事件（`PetAction` / `PetReaction` / `PetMode` / ...）。
2. 在 `PetAnimationResolver` 里产生候选动画名（基础循环），或在 `PetAction` / `PetReaction` 里加候选链（一次性触发）。
3. 在行为代码里触发 `triggerAction` / `triggerReaction`。
4. 确认资源文件里存在对应动画。
5. 装饰性循环：在子类 renderer 构造里 `addLoopingController(...)`。
6. 完全自定义需求：`addController(new ControllerConfig(name, blendMode, transitionSec, handler))`。
7. 只有当采样、混合或挂载模型无法表达需求时，才修改 renderer 主路径或 `PoseSampler`。

避免事项：

| 避免 | 原因 |
|---|---|
| 在 AI task 里直接写动画资源 key | 业务和资源强耦合 |
| 在 renderer 里判断职业和任务 | 渲染层会变成状态机 |
| 让一次性动画停留在最后一帧（除非显式 HOLD_ON_LAST_FRAME） | 会污染后续基础循环 |
| 为简单 head look-at 制作大量过渡动画 | 程序化骨骼更适合 |
| ADDITIVE controller 上 keyframe scale 通道而预期"加性" | scale 走 `*=`，不是 `+=` |

## 测试与验证

当前覆盖：

| 测试 | 目标 |
|---|---|
| `PetAnimationResolverTest` | 状态到候选列表的映射 |
| `ControllerInstanceTest` | playOnce 触发覆盖 / HOLD_ON_LAST_FRAME 不自动清 / ADDITIVE 按通道分别合成 |
| `LoopModeTest` | Bedrock `loop` 字段三态解析 |
| `PoseMixerTest` | crossfade 角度插值 |

常用命令：

```powershell
.\gradlew.bat :common:test --console=plain
.\gradlew.bat build --console=plain
.\gradlew.bat :neoforge:runClient
.\gradlew.bat :neoforge:runData
```

`runClient` 是视觉验收的关键，尤其要看：

1. GUI 预览实体是否不再倍速。
2. `idle/run/sit` 切换是否稳定（main controller 的 0.16s crossfade）。
3. 行为动作是否会播放后回到 base loop（playOnce → 自动清空）。
4. `RightHandLocator` 上的手持物是否跟随骨骼旋转和位置。
5. 装饰性 controller（blink/breath）启用后是否按预期叠加，且不破坏 base loop 的姿态。
6. 资源 `F3+T` 重载后宠物是否平滑切到新动画（BakeStamp 失效淘汰）。
