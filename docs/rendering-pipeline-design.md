# 自研渲染与动画管线设计

本文档描述 Chiikawa 模组脱离 Geckolib 后的自研 Bedrock 模型渲染与动画管线。目标是让模型、动画、物品挂载、程序化骨骼控制和服务器触发的一次性动作都由我们自己的轻量系统管理。

## 设计目标

1. 稳定支持 Minecraft 1.21.6 到 26.1.2 等多分支迁移。
2. 不依赖 Geckolib 的实体动画计时和控制器状态。
3. 消除 GUI 预览双 `extractRenderState` 导致的动画倍速和抖动。
4. 保留 Blockbench Bedrock Entity 工作流，方便动画师交付 `.geo.json` 和 `.animation.json`。
5. 把渲染层、动画采样层、宠物业务状态层解耦。
6. 为后续多层动作、反应动画、手持道具、乐器等扩展留下清晰入口。

## 总体结构

代码位于 `common/src/main/java/com/dwinovo/chiikawa/anim`，分为七个职责层：

```text
format -> compile -> molang -> baked -> runtime -> state -> render/api
```

| 层 | 主要职责 | 代表类 |
|---|---|---|
| `format` | 承接 Bedrock JSON 反序列化 | `BedrockGeoFile` |
| `compile` | 资源重载时把 JSON 烘焙成运行时数据 | `BedrockResourceLoader`, `ModelBaker`, `AnimationBaker` |
| `molang` | 编译和执行有限范围的 Molang 表达式 | `MolangCompiler`, `MolangContext`, `MolangNode` |
| `baked` | 不可变共享模型和动画数据 | `BakedModel`, `BakedAnimation`, `BakedBoneChannel` |
| `runtime` | 每个实体自己的播放通道和采样状态 | `PetAnimator`, `AnimationChannel`, `PoseSampler` |
| `state` | 把游戏状态映射为动画计划 | `PetAnimContext`, `PetAnimationResolver`, `PetAnimPlan` |
| `render/api` | Minecraft 客户端集成和业务侧接口 | `ChiikawaEntityRenderer`, `ModelRenderer`, `ChiikawaAnimated` |

原则：左侧层不依赖右侧层。`format`、`compile` 只在资源加载时运行；`baked` 只读共享；`runtime` 是每个实体一份；`state` 是纯决策；`render` 负责与 Minecraft 渲染 API 对接。

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
    -> BedrockResourceLoader
    -> Gson/JsonParser
    -> ModelBaker / AnimationBaker / MolangCompiler
    -> ModelLibrary.replaceAll / AnimationLibrary.replaceAll
```

烘焙后的注册表 key：

| 数据 | key 形式 |
|---|---|
| 模型 | `<namespace>:<pet_name>` |
| 动画 | `<namespace>:<pet_name>/<animation_name>` |
| 贴图 | `<namespace>:textures/entities/<pet_name>.png` |

这意味着业务层只需要产出动画名，例如 `idle`、`harvest`、`scratch_head`，renderer 会用当前模型 key 拼出完整动画 key。

## Baked 数据模型

`BakedModel` 是模型的不可变运行时表示：

| 字段 | 含义 |
|---|---|
| `bones` | 骨骼数组，包含 parent、pivot、rest rotation、cube range |
| `cubes` | cube 顶点和 UV 数据 |
| `rootBones` | 根骨骼索引 |
| `boneByName` | 骨骼名到索引的查找表 |
| `textureWidth/textureHeight` | Bedrock 贴图尺寸 |

`BakedAnimation` 是动画的不可变运行时表示：

| 字段 | 含义 |
|---|---|
| `name` | 动画短名 |
| `durationSec` | 动画时长，秒 |
| `looping` | 资源内声明的循环属性 |
| `channels` | 扁平化后的骨骼 channel |

`BakedBoneChannel` 会把同一根骨骼的 rotation、position、scale 拆成独立 channel。每个 channel 已经完成骨骼名解析、时间轴整理、插值模式记录和 Molang slot 编译。

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
PoseSampler.resetIdentity
    -> sample base channel
    -> sample action/reaction/overlay channels
    -> apply BoneInterceptor
    -> ModelRenderer.render
    -> RenderLayer registry (HeldItemLayer, ...)
```

后采样的 channel 会覆盖或叠加前面的骨骼 slot。目前采样器是直接写入模式，后续如果需要真正的 additive blend，可以在 `PetAnimator.Slot.OVERLAY` 上扩展混合策略。

## 纯函数采样

旧动画库的问题来自可变累计时间。我们的采样规则是：

```java
PoseSampler.sample(channel, nowNs, ctx, poseBuf)
```

`AnimationChannel` 只记录：

```java
record AnimationChannel(BakedAnimation animation, long startTimeNs, boolean looping)
```

采样时通过 `nowNs - startTimeNs` 计算本地动画时间。相同输入得到相同输出，没有 `lastAnimatableAge` 之类的可变累计字段。

这个约定直接解决了 GUI 预览实体时的双 extract 问题：

1. `extractRenderState` 可以被同一帧调用多次。
2. `setMain` 是幂等的，不会重复重启动画。
3. 采样时间来自 `System.nanoTime()` 和 channel 起点。
4. 重复 extract 不会让动画多走一遍。

## Animator 层级

`PetAnimator` 当前以 `Slot` 枚举划分：

| slot | 用途 |
|---|---|
| `BASE` | 基础循环动画，例如 `idle`、`run`、`sit` |
| `ACTION` | 行为动作，例如 `harvest`、`plant`、`slash` |
| `OVERLAY` | 预留给上半身或道具 overlay |
| `REACTION` | 情绪反应，例如 `happy`、`hurt`、`scratch_head` |

新增 slot 仅需在枚举末尾追加一个常量，调用方按符号引用、不依赖序号。

基础 slot 通过 `setMain(animation, true)` 驱动。它只在目标动画对象或 loop 属性变化时替换 channel。

一次性 slot 通过 `trigger(Slot, animation)` 驱动。非循环 channel 播放结束后由 `clearFinished(nowNs)` 清掉，避免动作停留在最后一帧。

## 渲染流程

`ChiikawaEntityRenderer.extractRenderState` 做快照：

1. 从 `LivingEntity` 读取 body rotation、head rotation、pitch、scale、walkSpeed。
2. 保存主手物品 `ItemStack`。
3. 调用 `ChiikawaAnimated.getAnimContext(walkSpeed)` 获取游戏状态快照。
4. 调用 `PetAnimationResolver.resolve(context)` 得到 `PetAnimPlan`。
5. 按 `baseLoopCandidates` 找到第一个存在的动画，并设置到 base layer。
6. 清理已结束的一次性 channel。
7. 把 main channel 和 sub channels 复制到 render state。

`submit` 做真正渲染：

1. 获取 `BakedModel`。
2. 创建并初始化 pose buffer。
3. 按 channel 采样动画；base layer 如果正在切换，会走通用 crossfade。
4. 运行程序化骨骼覆写。
5. 渲染模型 mesh。
6. 把主手物品挂到 `RightHandLocator`。

## 通用基础动画过渡

base layer 的切换不区分 `idle`、`run`、`sit` 或职业待机。任何 A -> B 都走同一套轻量 transition：

```text
PetAnimator.setMain(newAnimation)
    -> 如果当前 base 动画不同，记录 AnimationTransition(fromChannel, startTime, duration)
    -> 新 base channel 从 startTime 开始播放

submit
    -> sample fromChannel
    -> sample new channel
    -> PoseMixer.blend(fromPose, toPose, smoothstep(alpha))
```

默认过渡时长由 `PetAnimator.DEFAULT_BASE_TRANSITION_SEC` 控制。第一次设置 base animation 不创建 transition；同一个动画重复 `setMain` 是幂等的，不会重启计时。

`PoseMixer` 对 position/scale 做线性插值，对 rotation 做最短路径角度插值，避免 `179° -> -179°` 这类切换绕一大圈。

## 程序化骨骼干预

`BoneInterceptor` 是动画采样之后的程序化覆写点，按 `Stage`（`LOOK_AT` / `PHYSICS_SECONDARY` / `OCCLUSION`）分阶段运行。当前默认拆成两个互不干扰的 interceptor：

- `HeadLookInterceptor`（`Stage.LOOK_AT`）—— 头部 yaw/pitch 跟随玩家
- `IdleSwayInterceptor`（`Stage.PHYSICS_SECONDARY`）—— 耳朵摆动 + 尾巴摇

它们覆盖以下骨骼：

| 骨骼 | 用途 |
|---|---|
| `AllHead` | look-at/head pitch/head yaw |
| 耳朵骨骼 | 闲置摆动 |
| 尾巴骨骼 | 闲置摆动 |

设计意图是让“过渡、注视、微动”走程序化管线，而不是让动画师为每个状态都做大量过渡片段。动作师主要交付稳定可复用的基础循环、行为动作和情绪反应。

## 物品挂载

手持物挂载在骨骼：

```text
RightHandLocator
```

`HeldItemLayer`（注册在 `ChiikawaEntityRenderer.renderLayers` 列表里）会调用通用的 `BoneTransformWalker` 沿骨骼父链应用 rest transform 和 pose transform，最终把 item renderer 提交到该 locator。

新增挂件（道具、披风、发光眼睛等）只需实现 `RenderLayer` 接口，由 renderer 子类在构造函数里 `addRenderLayer(...)` 注册，无需改主路径。

注意：模型 PoseStack 已经处于 `1/16` pixel 缩放后空间，但 Minecraft item renderer 使用块单位。`HeldItemLayer` 在提交物品前会恢复到块单位，避免物品尺寸异常。

## 与状态机的边界

渲染管线不直接理解宠物 AI 和任务。它只消费 `PetAnimPlan`：

```text
PetAnimContext -> PetAnimationResolver -> PetAnimPlan -> AnimationLibrary lookup -> PetAnimator channels
```

因此新增一个任务动作时，优先改状态机枚举和触发点，而不是在 renderer 里写业务判断。

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

行为动作：

| 名称 | fallback |
|---|---|
| `pickup` | `use_mainhand` |
| `harvest` | `use_mainhand` |
| `plant` | `use_mainhand` |
| `deposit` | `use_mainhand` |
| `slash` | `use_mainhand` |
| `bow_draw` | `sword_attack` |
| `bow_release` | `sword_attack` |

情绪反应：

| 名称 | fallback |
|---|---|
| `happy` | 无 |
| `hurt` | 无 |
| `scratch_head` | `confused` |
| `revive` | `happy` |

fallback 让旧资源能够继续工作。新增动画时优先用语义名称，旧名只作为兼容层保留。

## 扩展规则

新增动画能力时优先按以下顺序扩展：

1. 新增语义状态或事件。
2. 在 `PetAnimationResolver` 里产生候选动画名。
3. 在行为代码里触发 `triggerAction` 或 `triggerReaction`。
4. 确认资源文件里存在对应动画。
5. 只有当采样、混合或挂载模型无法表达需求时，才修改 renderer。

避免事项：

| 避免 | 原因 |
|---|---|
| 在 AI task 里直接写动画资源 key | 业务和资源强耦合 |
| 在 renderer 里判断职业和任务 | 渲染层会变成状态机 |
| 让一次性动画停留在最后一帧 | 会污染后续基础循环 |
| 为简单 head look-at 制作大量过渡动画 | 程序化骨骼更适合 |

## 测试与验证

当前覆盖：

| 测试 | 目标 |
|---|---|
| `PetAnimationResolverTest` | 状态到动画候选列表的映射 |
| `PetAnimatorTest` | 一次性 channel 完成判断和清理 |

常用命令：

```powershell
.\gradlew.bat :common:test --console=plain
.\gradlew.bat build --console=plain
.\gradlew.bat :neoforge:runClient
.\gradlew.bat :neoforge:runData
```

`runClient` 是视觉验收的关键，尤其要看：

1. GUI 预览实体是否不再倍速。
2. `idle/run/sit` 切换是否稳定。
3. 行为动作是否会播放后回到 base loop。
4. `RightHandLocator` 上的手持物是否跟随骨骼旋转和位置。
5. 新增反应动画资源后，驯服、喂食、受伤、复活是否播放。
