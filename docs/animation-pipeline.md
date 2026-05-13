# 自有 Bedrock 动画管线

自有渲染/动画管线，处理 7 只 chiikawa 宠物的所有动画播放、骨骼程序化干预、手持物挂载和服务器触发同步。架构对齐 GeckoLib 的多 controller 模型，但保留我们自己的纯函数采样器和扁平 pose buffer。

## 触发原因

旧外部实体动画库在新版渲染状态抽取路径下有系统性双采样问题：控制器的可变累积时间会在每次 `extractRenderState` 调用时前进。`InventoryScreen.renderEntityInInventoryFollowsMouse` 在 GUI 预览实体时每帧多触发一次 extract，结果是动画约 2 倍速 + 移动时抖动。

主流 mod 里没有"旧动画实体 + 容器 GUI 实时预览"的可复用先例，所有 workaround 都强耦合旧库内部。所以我们做了自己的管线。

## 架构

九层，依赖箭头从左到右：

```
format → compile → molang → baked → controller → runtime → state → render → api
 POJO    bake-time          immut    config       per-      MC       外部
                            shared                entity   integration  接口
```

每层只依赖左侧。`format`/`compile` 仅在资源加载时跑；`baked` 是不可变共享数据；`controller` 是配置类型 + 每实体可变状态；`runtime` 持有每实体动画时间线；`render` 是 Minecraft 集成；`api` 是业务代码看到的入口。

### 文件分布

```
common/src/main/java/com/dwinovo/chiikawa/anim/
├── format/                       # 第 1 层：磁盘 → POJO
│   └── BedrockGeoFile.java       #   .geo.json Gson 反序列化目标
│
├── compile/                      # 第 2 层：POJO → Baked（资源加载时一次性）
│   ├── ModelBaker.java           #   .geo.json → BakedModel
│   ├── AnimationBaker.java       #   .animation.json → BakedAnimation
│   ├── MolangCompiler.java       #   Molang 字符串 → MolangNode AST
│   └── BedrockResourceLoader.java#   ResourceManagerReloadListener，挂在 client 入口
│
├── molang/                       # 第 3 层：Molang 引擎
│   ├── MolangNode.java           #   sealed AST：Const/Var/Neg/Add/Sub/Mul/Div/FuncCall
│   ├── MolangContext.java        #   2 个 double slot（anim_time / ground_speed）
│   └── MolangFn.java             #   5 个内置函数 dispatch
│
├── baked/                        # 第 4 层：不可变只读运行时数据
│   ├── BakedModel.java           #   骨骼数组 + cube 数组 + name→idx
│   ├── BakedBone.java            #   parentIdx, pivot, restRot, cubeRange, children
│   ├── BakedCube.java            #   minX/maxX/...、pivot、rot、6×4×2 UV
│   ├── BakedAnimation.java       #   name, duration, loopMode, channels[]
│   ├── BakedBoneChannel.java     #   boneIdx, type, times[], values[], lerpModes[], molangSlots[]
│   ├── LoopMode.java             #   PLAY_ONCE / LOOP / HOLD_ON_LAST_FRAME
│   └── BakeStamp.java            #   重载代际计数
│
├── controller/                   # 第 5 层：GeckoLib 风格 controller 模型
│   ├── BlendMode.java            #   OVERRIDE / ADDITIVE
│   ├── ControllerHandler.java    #   (state, ctx) -> BakedAnimation? 纯函数
│   ├── ControllerConfig.java     #   record(name, blendMode, transitionSec, handler)
│   ├── ControllerInstance.java   #   每实体 mutable 状态（含 playOnce 触发覆盖）
│   └── ControllerSnapshot.java   #   每帧不可变快照，喂给 submit pass
│
├── runtime/                      # 第 6 层：动画时间线 + 采样
│   ├── PetAnimator.java          #   List<ControllerInstance> 容器
│   ├── AnimationChannel.java     #   record(BakedAnimation, startTimeNs, looping)
│   ├── PoseSampler.java          #   纯函数：(channel, mode, nowNs, ctx, buf) → 写入 buf
│   └── PoseMixer.java            #   pose 缓冲混合 + smoothstep alpha
│
├── state/                        # 第 7 层：游戏状态 → 候选动画名
│   ├── PetAnimContext.java       #   每帧状态快照（mode/job/locomotion/action/reaction/attention）
│   ├── PetAnimationResolver.java #   纯函数：context → List<String> 候选名
│   ├── PetAction.java            #   行为动作事件 + network id
│   ├── PetReaction.java          #   情绪反应事件 + network id
│   ├── PetMode.java              #   FOLLOW / SIT / WORK
│   ├── PetJobRole.java           #   NONE / FARMER / FENCER / ARCHER
│   ├── PetLocomotion.java        #   IDLE / WALK / RUN
│   └── PetAttention.java         #   注视目标类型（预留）
│
├── render/                       # 第 8 层：Minecraft 集成
│   ├── ChiikawaEntityRenderer.java # extends EntityRenderer<T, ChiikawaRenderState>
│   ├── ChiikawaRenderState.java    # 携带 modelKey/texture/controllerSnapshots[]/heldItem
│   ├── ModelRenderer.java          # 骨骼 DAG 遍历 + cube quad 发射
│   ├── BoneInterceptor.java        # 骨骼程序化覆写接口（带 Stage 枚举）
│   ├── HeadLookInterceptor.java    # Stage.LOOK_AT 默认实现（AllHead）
│   ├── IdleSwayInterceptor.java    # Stage.PHYSICS_SECONDARY 默认实现（耳/尾）
│   ├── BoneInputProvider.java      # MolangContext per-frame filler 接口
│   ├── BasicMolangInputProvider.java # 默认 ground_speed filler
│   ├── layer/                       # RenderLayer 注册表
│   │   ├── RenderLayer.java         # 视觉层接口
│   │   ├── RenderLayerContext.java  # per-submit 数据包
│   │   ├── BoneTransformWalker.java # 骨骼父链 PoseStack 推进工具
│   │   └── HeldItemLayer.java       # 物品挂载到 RightHandLocator
│   └── impl/                       # 7 个 trivial 子类（ChiikawaRenderer 等）
│
└── api/                          # 第 9 层：业务接口
    ├── ModelLibrary.java         #   namespace:path → BakedModel 注册表
    ├── AnimationLibrary.java     #   namespace:path/anim → BakedAnimation 注册表
    └── ChiikawaAnimated.java     #   实体侧接口：getPetAnimator() + getAnimContext()
```

## 核心数据流

### 加载时（每次资源包重载触发一次）

```
.geo.json + .animation.json
    │
    ▼ Gson + JsonParser
BedrockGeoFile + 原始 JsonObject
    │
    ▼ ModelBaker / AnimationBaker / MolangCompiler
BakedModel + BakedAnimation + MolangNode AST  （都打上当前 BakeStamp）
    │
    ▼ ModelLibrary.replaceAll / AnimationLibrary.replaceAll
[共享只读注册表]
```

由 [`BedrockResourceLoader`](../common/src/main/java/com/dwinovo/chiikawa/anim/compile/BedrockResourceLoader.java) 驱动，挂在 `ChiikawaFabricClient.onInitializeClient` 和 NeoForge 的等价位置。

### 每帧（每只可见的宠物）

```
ChiikawaEntityRenderer.extractRenderState
    │
    ├─ 从 LivingEntity 读 bodyRot / yRot / xRot / walkSpeed / mainHandItem
    ├─ 把 head_yaw / head_pitch snapshot 到 state（避开 InventoryScreen 后续覆写）
    ├─ animator.ensureInitialised(controllerConfigs)   ← 首帧懒构建 controller instances
    ├─ animator.setPhaseSeed(uuid, nowNs)              ← 首次锁存相位偏移
    ├─ animator.clearStale(currentModel.bakeStamp)     ← 资源重载后清失效引用
    └─ animator.tick(state, ctx, nowNs)
       └─ for each ControllerInstance:
            ├─ 若 triggered 还活着：维持 triggered animation
            ├─ 否则调 handler(state, ctx) → 拿到目标动画
            └─ 切换时启动 crossfade（按 transitionSec）
    │
    ▼ state.controllerSnapshots = animator.snapshot()  ← 不可变快照数组
    │
ChiikawaEntityRenderer.submit
    │
    ├─ 分配 poseBuf = float[boneCount * 9]
    ├─ PoseSampler.resetIdentity(poseBuf)
    ├─ 填 MolangContext.vars (ground_speed)
    ├─ for each ControllerSnapshot：                   ← 注册序 = 优先级
    │     按 BlendMode 调 PoseSampler.sample 写入 poseBuf
    │     OVERRIDE controller 处于 fade 中：双缓冲 sample + PoseMixer.blend
    ├─ for each interceptor stage（LOOK_AT / PHYSICS_SECONDARY / OCCLUSION）：
    │     interceptor.apply(model, state, ctx, poseBuf)  [程序化覆写]
    │
    ├─ poseStack.rotateY(180 - bodyRot) + scale(1/16)
    ├─ collector.submitCustomGeometry(deferred ModelRenderer.render call)
    └─ 各 RenderLayer.submit（HeldItemLayer 等）
```

## 关键不变量

### Pose buffer 布局

`float[boneCount * 9]`，每根骨骼 9 个 slot：

| 索引 | 含义 | identity |
|---|---|---|
| `[b*9+0..2]` | 旋转 delta（XYZ Euler，弧度） | 0, 0, 0 |
| `[b*9+3..5]` | 位置 offset（pixel 单位） | 0, 0, 0 |
| `[b*9+6..8]` | scale（multiplier） | 1, 1, 1 |

每帧由 `PoseSampler.resetIdentity` 清零再 sample 写入。常量见 [`PoseSampler.OFFSET_*`](../common/src/main/java/com/dwinovo/chiikawa/anim/runtime/PoseSampler.java)。

### X 镜像约定

Blockbench 导出 `.geo.json` 时把 display +X 翻转成 JSON -X（Bedrock 历史包袱）。我们在 **bake 阶段** 一次性对消：

| 字段 | 处理 |
|---|---|
| bone.pivot.x、cube.origin.x（变成 `-(o.x + s.x)`）、cube.pivot.x、cube.rotation.x | 取反 |
| bone.rest.rotX、bone.rest.rotY、cube.rotation.y | 取反 |
| bone.rest.rotZ、cube.rotation.z | 不变 |
| Animation rotation channel: X、Y 取反，Z 不变 | 同上 |
| Animation position channel: X 取反 | 同 pivot |
| Animation scale channel | 不变（镜像对称） |
| 非 mirror cube 的 face UV 的 U 翻转 | 补偿几何镜像 |

参见 [`ModelBaker.bakeCube`](../common/src/main/java/com/dwinovo/chiikawa/anim/compile/ModelBaker.java) 和 [`AnimationBaker.applyMirrorAndUnits`](../common/src/main/java/com/dwinovo/chiikawa/anim/compile/AnimationBaker.java)。

**渲染器不再做任何 X 镜像**，只做 `rotateY(180 - bodyRot)` 把模型对齐到 Minecraft 实体朝向。

### 采样是纯函数（双采样问题的根本修复）

```java
PoseSampler.sample(channel, blendMode, animationTimeNs, ctx, poseBuf)
```

是引用透明的：相同输入永远相同输出，**没有任何累积时间状态**。`AnimationChannel` 是 `record(BakedAnimation, long startTimeNs, boolean looping)`。每次采样都从 `nowNs - startTimeNs` 重新算，不存"上次走到哪了"。

`animationTimeNs` 来自 `tickCount + partialTick` 经 `AnimationClock` 转换后的游戏时间，而不是系统真实时间。这样 InventoryScreen 在同一帧调两次 extract 时会拿到同一份时间快照，采样输出 bit-identical → 物理性消除双 extract 漂移；单机 ESC 真暂停时 vanilla 会冻结 partial tick，动画也随之冻结。

## Controller 模型（GeckoLib 对齐）

每只宠物的渲染器在构造函数里注册一个 `ControllerConfig` 列表。每个 config 包含：

| 字段 | 含义 |
|---|---|
| `name` | controller 标识，外部 `playOnce(name, anim)` 用这个寻址 |
| `blendMode` | `OVERRIDE`（默认）或 `ADDITIVE` |
| `transitionSec` | handler 切换动画时的 crossfade 长度 |
| `handler` | `(state, ctx) -> BakedAnimation?` 纯函数；返回 `null` = 该帧无贡献 |

**注册序就是优先级**：后注册的 controller 在 sample 时晚一步写 poseBuf，`OVERRIDE` 模式下覆盖前者，`ADDITIVE` 模式下叠加。这是 GeckoLib 的核心思路。

### 默认注册的 controllers

`ChiikawaEntityRenderer` 基类构造函数自动注册三个：

| name | blendMode | transition | handler 行为 |
|---|---|---|---|
| `main` | OVERRIDE | 0.16s | 调 `PetAnimationResolver.resolve(ctx)`，取候选链第一个存在的动画 |
| `action` | OVERRIDE | 0.15s | `null`（永不主动出力，只响应 `playOnce`） |
| `reaction` | OVERRIDE | 0.15s | `null`（永不主动出力，只响应 `playOnce`） |

`action` / `reaction` 的 `transitionSec` 不为 0 是**故意的**——`playOnce` 触发的 fade-IN 在代码里硬编码为 `allowFade=false`（gameplay 事件应该立刻可见），但播完后的 fade-OUT 会用这个值，让 harvest/slash 等动作收尾时平滑过渡回 `main` 的 base loop。

子类可以追加装饰性 controllers，例如：

```java
public ChiikawaRenderer(EntityRendererProvider.Context ctx) {
    super(ctx, "chiikawa");
    addLoopingController("blink",  BlendMode.OVERRIDE, "blink");
    addLoopingController("breath", BlendMode.ADDITIVE, "breath");
}
```

`addLoopingController` 是 `addController(...)` 的语法糖：注册一个 `transitionSec=0`、handler 永远返回指定循环动画的 controller。

### Blend mode 数学（对齐 GeckoLib `AnimationProcessor`）

`PoseSampler` 在 ADDITIVE 模式下按 channel 类型分别合成（**这是从 GeckoLib 学到的关键点**）：

| 通道 | OVERRIDE | ADDITIVE |
|---|---|---|
| rotation | `pose[i] = animValue` | `pose[i] += animValue` |
| position | `pose[i] = animValue` | `pose[i] += animValue` |
| scale | `pose[i] = animValue` | `pose[i] *= animValue` ← 注意是乘，不是加 |

scale 必须是乘法因为 identity 是 `1.0`，朴素 `+=` 会导致 `1+1=2` 每帧爆炸。

动画文件**没 keyframe 的通道完全不动 poseBuf**，无论什么 blend mode。这意味着动画师只在他们想贡献的通道上 keyframe，自然就拿到了想要的合成结果——不需要专门的 `ROTATION_ONLY` 之类的细分模式。

### Triggered animation（一次性覆盖）

`ControllerInstance.playOnce(animation, nowNs)` 把外部一次性动画塞进 controller，**优先级高于 handler**：在动画的 baked 时长内，handler 被完全跳过，sample 该 controller 出的就是 triggered animation。时长结束后自动清空，handler 恢复决策。

`LoopMode.HOLD_ON_LAST_FRAME` 触发的动画不会自动清，要靠 `playOnce` 替换或 `clearTrigger()` 显式清掉。

这是 [`AbstractPet`](../common/src/main/java/com/dwinovo/chiikawa/entity/AbstractPet.java) 同步 action/reaction 触发包的接收端：

```
server task → pet.triggerAction(PetAction.HARVEST)
           → 同步字段 ANIM_TRIGGER 高 24 位 seq +1，低 8 位写 networkId
           → 客户端 onSyncedDataUpdated
           → 解析 networkId 找 BakedAnimation
           → animator.playOnce("action", anim, AnimationClock.fromTicks(tickCount, 0))
```

`reaction` 走平行的 `REACTION_TRIGGER` 字段和 `playOnce("reaction", anim, nowNs)`。

### 过渡机制（两种 fade，全部由代码自动处理）

每个 controller 有一个 `transitionSec`，同时控制两类 fade：

**1. 单 controller 内部切换**（`previous` + `current` 同时存在）

handler 在 idle → sit 之间切换时：
- `previous` = 旧 idle channel，`current` = 新 sit channel
- 两个 channel 都从当前 `poseBuf` 状态出发各自采样到 temp 缓冲（这样无关骨头不被搅乱）
- `PoseMixer.blend` 按 smoothstep alpha 把两个 temp 缓冲合并写回 poseBuf
- alpha 走完 0→1 后 `previous` 释放，`current` 独占

OVERRIDE 和 ADDITIVE 走同一条路径——ADDITIVE 的"采样"是 `+=`/`*=`，做完后 temp 缓冲里的就是"叠加完的姿态"，再 lerp 也是有效的。

**2. controller 整体停止**（`fadingOut = true`）

handler 返回 `null`、或 `playOnce` 触发的动画播完时，controller 不会立即清空，而是进入 stop-fade：
- `current` 还指着最后那段动画，继续被采样
- 但采样写完 poseBuf 后，按 alpha lerp **回到 sample 之前的 poseBuf 状态**
- alpha 0 = 完整贡献；alpha 1 = 完全消失，下层 controller 写的姿态完整透出
- fade 结束后 `current` 才真正清空

这就是跨 controller 的自动过渡：`action` controller 的 harvest 播完后，会在 0.15 秒内从 harvest 末态平滑过渡到 `main` 当前的 idle/sit/run，**动画师不用做任何收尾工作**。

**关键 BUG（已修复）**：`fadeStartNs` 必须等于 `nowNs`，绝不能等于新动画的 `startNs`。对循环动画 `startNs = phaseSeed`（启动时锁定的过去时间），如果用错会让 fade alpha 在第一帧就 clamp 到 1.0，所有切换变成硬切。回归测试见 [`ControllerInstanceTest.withinControllerFadeUsesNowNsNotPhaseSeed`](../common/src/test/java/com/dwinovo/chiikawa/anim/controller/ControllerInstanceTest.java)。

### MolangContext 范围

只有两个 slot 真有值：

| slot | 来源 |
|---|---|
| `query.anim_time` / `q.anim_time` | `PoseSampler.sample` 每个 channel 采样前填，等于该 channel 的本地时间 |
| `query.ground_speed` | `BasicMolangInputProvider` 每帧从 `walkAnimation.speed` 填 |

**故意不暴露的**：

| Molang 引用 | 状态 | 替代方案 |
|---|---|---|
| `ysm.head_yaw` / `ysm.head_pitch` | 软失败 → `Const(0)` | `HeadLookInterceptor` 程序化控制 AllHead 骨骼 |
| `v.L6_P0` / `v.L4_P0` / `v.L6_P00` | 软失败 → `Const(0)` | 这些是 Blockbench IK 导出残留，没有 SET 站点 |

详见 [`MolangContext`](../common/src/main/java/com/dwinovo/chiikawa/anim/molang/MolangContext.java) 顶部注释。

`MolangCompiler` 对未知变量/函数 `warn` 一次然后返回 `Const(0)`，不抛异常 —— 一个表达式坏不影响整个动画加载。

### 单位约定

PoseStack 在我们的渲染流水线里始终处于 **1/16-scaled pixel 空间**：
- `scale(1/16)` 已在 `submit` 阶段应用
- 骨骼 pivot 用原始 pixel 数值，可直接 `translate(pivotX)`
- ModelRenderer 顶点也直接用原始 pixel 数值

但 **物品** 在 Mojang 的 API 里使用块单位（BakedQuad vertex 是 0..1 块，display transform 也是块为单位）。所以 `HeldItemLayer.submit` 在 chain walk 终点会 `scale(16, 16, 16)` 抵消我们的 1/16，恢复块单位 → 物品才能以正常尺寸渲染。

vanilla 不踩这个坑是因为 `LivingEntityRenderer` 把 1/16 因子放在 `ModelPart` 顶点生成里，PoseStack 始终块单位。我们的约定相反，要在物品边界处显式抵消。

## 资产约定

### 路径

```
common/src/main/resources/assets/<namespace>/
├── models/entity/<pet_name>.json    ← Bedrock geometry，format_version 1.12.0
├── animations/<pet_name>.json        ← Bedrock animation，format_version 1.8.0
└── textures/entities/<pet_name>.png  ← 贴图
```

文件名 `<pet_name>` 必须等于 EntityType 的注册名 path（`InitEntity.registerPet("chiikawa", ...)` 对应 `chiikawa.json`）。

### Blockbench 导出选项

- 导出格式：**Bedrock Entity**（不是 Java Block / Item）
- format_version `1.12.0`（geometry）/ `1.8.0`（animation）
- 不要勾选 "decimals trimming" 之类的精度优化（会让浮点等于 0 检测失败）
- 一个 `.geo.json` 里只取第一个 `minecraft:geometry` 条目（`identifier: geometry.unknown` 即可，我们不依赖它）

### 必须存在的骨骼名

[`HeadLookInterceptor`](../common/src/main/java/com/dwinovo/chiikawa/anim/render/HeadLookInterceptor.java) 与 [`IdleSwayInterceptor`](../common/src/main/java/com/dwinovo/chiikawa/anim/render/IdleSwayInterceptor.java) 默认实现按名查找：

| 骨骼名 | 用途 | 缺失时 |
|---|---|---|
| `AllHead` | 头部 yaw/pitch 跟随玩家 | 跳过，pet 头不动 |
| `LeftEar` / `RightEar` | 闲置摆动 + 跑动后压 | 跳过，耳朵静止 |
| `tail` | Y 轴轻微摇摆 | 跳过，尾巴静止 |
| `RightHandLocator` | 手持物挂载锚点 | 物品不显示 |

模型缺这些骨骼**不会崩**，只是对应的程序化效果失效。

### 必须存在的动画名

[`PetAnimationResolver`](../common/src/main/java/com/dwinovo/chiikawa/anim/state/PetAnimationResolver.java) 按状态产出候选动画名。最小资源集：

| 动画名 | 何时播放 |
|---|---|
| `idle` | 默认状态，也是最终 fallback |
| `run` | walkSpeed > 0.15 |
| `sit` | PetMode == SIT |
| `use_mainhand` | 旧通用主手动作 fallback |
| `sword_attack` | 旧通用攻击动作 fallback |

推荐新增语义动画名：

| 类型 | 动画名 |
|---|---|
| 工作待机 | `work_idle_farmer`, `work_idle_fencer`, `work_idle_archer` |
| 行为动作 | `pickup`, `harvest`, `plant`, `deposit`, `slash`, `bow_draw`, `bow_release` |
| 情绪反应 | `happy`, `hurt`, `scratch_head`, `confused`, `revive` |
| 装饰循环 | `blink`, `breath`, `tail_idle`（由 controller 注册启用） |

详见 [`pet-state-machine-design.md`](./pet-state-machine-design.md) 的命名契约。

## 如何扩展

### 添加新宠物

1. 添加 `<pet_name>.geo.json` + `<pet_name>.animation.json` + `<pet_name>.png`
2. 创建 `XxxPet.java` 继承 `AbstractPet`（重写 `getSoundSet`/`getReviveDollItem` 等）
3. `InitEntity.registerPet("xxx", XxxPet::new)` 注册
4. `common/src/main/java/com/dwinovo/chiikawa/anim/render/impl/XxxRenderer.java`：
   ```java
   public class XxxRenderer extends ChiikawaEntityRenderer<XxxPet> {
       public XxxRenderer(EntityRendererProvider.Context ctx) {
           super(ctx, "xxx");
       }
   }
   ```
5. 在 `ChiikawaFabricClient` 和 `ChiikawaClient` 注册 `EntityRenderers.register(InitEntity.XXX_PET.get(), XxxRenderer::new)`

### 添加新 Molang 变量

1. `MolangContext.java` 加 `SLOT_FOO = N`，`SLOT_COUNT++`
2. `MolangContext.resolveSlot` 的 switch 加 case
3. 写一个 `BoneInputProvider`（或扩展现有的 `BasicMolangInputProvider`）在 `fill` 里填 `ctx.vars[SLOT_FOO] = ...`
4. 在 renderer 子类构造函数里 `addInputProvider(new YourProvider())`（默认提供者已经在基类注册）

不要再直接在 `submit` 里改 `ctx.vars` —— 那会让"哪个变量谁负责"散落各处。

### 添加新触发动画

1. 在 [`PetAction`](../common/src/main/java/com/dwinovo/chiikawa/anim/state/PetAction.java) 或 [`PetReaction`](../common/src/main/java/com/dwinovo/chiikawa/anim/state/PetReaction.java) 中新增语义事件和 network id（低 8 位，0 是保留）。
2. 给事件配置有序动画候选名，例如 `"play_guitar", "use_mainhand"`。
3. server 端逻辑里调 `pet.triggerAction(PetAction.X)` 或 `pet.triggerReaction(PetReaction.X)`。
4. 重新打包后客户端会自动响应：`onSyncedDataUpdated` 解码事件，从候选中找第一个存在的动画，调 `animator.playOnce("action", anim, nowNs)` 或 `playOnce("reaction", anim, nowNs)`。

### 添加装饰性 controller（眨眼、呼吸、尾巴常摆）

1. 在 `<pet>.animation.json` 里加好 looping 动画，且**只 K 该效果涉及的 bone**（例如 `blink` 只动 `eyelid`）。
2. 在该宠物的 `XxxRenderer` 构造函数里追加 controller：
   ```java
   addLoopingController("blink",  BlendMode.OVERRIDE, "blink");
   addLoopingController("breath", BlendMode.ADDITIVE, "breath");
   ```
3. `OVERRIDE` 让 controller 在共享骨骼上完全覆盖前面的 controller；`ADDITIVE` 让其叠加（rot/pos `+=`、scale `*=`）。
4. 装饰性循环天然按 UUID 错开相位（`PetAnimator.setPhaseSeed`），不会一群宠物同时眨眼。
5. 重新加载（`F3+T` 或重启）。

### 添加完全自定义 controller

如果默认 main / action / reaction 三个 controller 不够，可以直接 `addController(new ControllerConfig(...))`。Handler 是纯函数，可以读 `state` 任何字段、`ctx` 任何状态：

```java
addController(new ControllerConfig(
    "mood_idle",
    BlendMode.ADDITIVE,
    0.3f,
    (state, ctx) -> ctx.attention() == PetAttention.OWNER
            ? AnimationLibrary.get(animKey("look_at_owner"))
            : null   // 不出力，让前面 controller 接管
));
```

handler 返回 `null` 表示该帧无贡献——poseBuf 保持前序 controllers 写入的状态。

### 添加新 BoneInterceptor

如果需要新的程序化骨骼覆写（比如尾巴随心情摆动幅度变化、SpringBone 二级动画）：

1. 实现 [`BoneInterceptor`](../common/src/main/java/com/dwinovo/chiikawa/anim/render/BoneInterceptor.java) 接口（`@FunctionalInterface`，一个方法）
2. 选定阶段：
   - `Stage.LOOK_AT` — 由外部目标驱动的 IK / 视线类（头、眼）
   - `Stage.PHYSICS_SECONDARY` — 由时间驱动的二级运动（耳、尾、SpringBone）；可读取 LOOK_AT 阶段写入的结果
   - `Stage.OCCLUSION` — 可见性 / 隐藏类（emote 期间隐藏某 bone、装备遮挡）
3. 在 `ChiikawaEntityRenderer` 子类构造函数里 `addInterceptor(stage, new YourInterceptor())`
4. 同一阶段内按注册顺序运行，**后写覆盖前写**，跨阶段按 `Stage` 枚举声明顺序运行

interceptor 在 **所有 controller 之后** 跑，是终极覆盖——head look-at 永远赢，无论动画里头怎么写。

### 修改 main loop 状态机

基础循环由 [`AbstractPet.getAnimContext(walkSpeed)`](../common/src/main/java/com/dwinovo/chiikawa/entity/AbstractPet.java) 产出状态快照，再由 [`PetAnimationResolver.resolve`](../common/src/main/java/com/dwinovo/chiikawa/anim/state/PetAnimationResolver.java) 选择候选动画名。通常只改 resolver，不在 renderer 里写业务判断。

## 常见坑

记录的是**已经踩过、改过、值得后人警惕**的具体场景。

### GUI extract 之后 InventoryScreen 会覆写 state.bodyRot/yRot/xRot

`InventoryScreen.renderEntityInInventoryFollowsMouse` 调用顺序：
1. `renderer.createRenderState(entity, partialTick)` —— 我们的 extract 跑完
2. **直接覆写** `state.bodyRot = 180 + f*20`、`state.yRot = f*20`、`state.xRot = -g*20`（鼠标驱动）

这意味着 `state.yRot - state.bodyRot = -180`（永远）。任何在 submit 阶段从 state 字段反推 entity 真实状态的代码都会拿到错值。

**修法**：所有派生量在 extract 阶段就 snapshot 到 state 自己的字段（如 `state.netHeadYaw`、`state.headPitch`），submit 阶段读 snapshot。

### `ysm.head_yaw` / `ysm.head_pitch` 是 vestigial

动画文件可能引用 `ysm.head_yaw`（来自 Yes Steve Model mod 的命名空间），但旧管线也从来没真填过这个变量。头部跟随**一直是渲染器程序化覆写** AllHead 骨骼实现的，不是 Molang。

**如果不小心填了真值**，跑动 + GUI 鼠标就会让 head_yaw 跑到 ±180，配合 `Root.rotZ = 0.4*ysm.head_yaw` 这种动画表达式会让整只宠物侧躺 72°。

**修法**：`MolangContext` 里**不要**给 ysm.* 加 slot。让 `MolangCompiler` 软失败成 `Const(0)`。头交给 `HeadLookInterceptor`，耳/尾交给 `IdleSwayInterceptor`。

### 物品在 1/16-scaled PoseStack 里直接 submit 会变 5cm 小

我们的 PoseStack 是 1/16 scale 空间（pixel 单位），但 `ItemStackRenderState.submit` 内部 vertex 是 0..1 块单位。直接 submit 物品会被额外乘 1/16 → 0.85 块的剑变成 5cm。

**修法**：`HeldItemLayer` 在 chain walk 终点 `scale(16, 16, 16)` 抵消。

### Additive 控制器不要对 scale 通道用 `+=`

`PoseSampler` 在 ADDITIVE 路径上对 scale 走 `*=`（乘法），其他通道走 `+=`（加法）。修改 sampler 时务必保留这个分叉——朴素全 `+=` 会让两个 ADDITIVE controller 同时贡献 scale 时模型每帧成倍放大。

参见 [`PoseSampler.writeBlended`](../common/src/main/java/com/dwinovo/chiikawa/anim/runtime/PoseSampler.java)。

### 浮点 identity 比较用 `== 0f`

[`AnimationBaker.isConstantIdentity`](../common/src/main/java/com/dwinovo/chiikawa/anim/compile/AnimationBaker.java) 和 [`ModelRenderer.renderBone`](../common/src/main/java/com/dwinovo/chiikawa/anim/render/ModelRenderer.java) 都用精确等于 `== 0f` / `== 1f` 判断 identity。如果未来出现"动画文件里写的是 `1e-10`，浮点不精确等于 0"导致剪枝失败，可以加个 epsilon（比如 `Math.abs(x) < 1e-6`）。

目前 Blockbench 导出的常量值都是规整的 `0` 或 `1`，没问题。

### 多实体共享 renderer 实例 + deferred submitCustomGeometry lambda

`submitCustomGeometry` 的 lambda 不在 submit 调用时执行，而是延迟到 batch 渲染。如果多个 entity 共享 renderer 实例，把 pose buffer 缓存在 renderer 上会被后一个 entity 的 submit 覆写，前一个 lambda 跑时拿到错的数据。

**修法**：每次 submit **新分配** pose buffer。代价是 ~864 字节/调用 × 几千次/秒 ≈ 几 MB/秒 GC 压力，可接受。

### `BakeStamp` 必须穿透到 controller 失效淘汰

资源重载会把 `ModelLibrary` / `AnimationLibrary` 替换成新的 `BakedModel` / `BakedAnimation` 对象，但每只宠物的 `ControllerInstance` 持有的引用还是旧代。`extract` 顶部的 `animator.clearStale(currentModel.bakeStamp)` 会扫每个 controller 的 current/previous/triggered，发现旧代直接整 controller 重置。新代由下一帧 handler 重新 pick。

如果跳过这一步，sampler 会拿到一个 boneIdx 已经无效的旧 channel，可能直接越界。

## 性能现状

参考 `git log` 的最后一个 `perf(anim):` 提交。当前优化：

1. **AnimationBaker bake 时丢弃 identity 通道**（all-zero rotation/position、all-one scale、所有 keyframe 都是 identity 的）
2. **ModelRenderer 跳过 identity 骨骼的 push/pop + pivot 三明治**（24 骨骼里大概 18 个是组织性的）
3. **BoneTransformWalker 链式遍历应用同样的 fast path**

热路径主要时间花在 Mojang 的 `BufferBuilder.addVertex`，我们的 Java 计算占整体 5-10%。Rust JNI 化 sampling 实测**会更慢**（boundary overhead 远超优化收益），所以不做。

下一批候选优化（按 ROI 从高到低，未实施）：

| 优化 | 预期收益 | 复杂度 |
|---|---|---|
| 距离 LOD（远处宠物每 N 帧采样一次 + 缓存 pose） | 远观大量 pet 时 N× | 中 |
| Bake 时折叠 no-op 骨骼（`MAllBody/AllBody/...` 上提到父） | 模型层级压扁，省 push/pop | 中 |

不要为了优化而优化 —— 当前没有性能信号触发以上改动。

## 关键设计原则（事后回看）

1. **采样是纯函数** —— 双采样类问题在新管线物理上不可能发生
2. **数据导向布局**（DOD/SoA） —— Java JIT 也喜欢，不依赖 Rust 化也比传统 OO 风格快 2-3 倍
3. **多 controller + per-controller blend mode** —— 对齐 GeckoLib 的成熟模型，比之前的 4 槽 + YSM-parallel 双系统更统一、扩展更便宜
4. **分层架构 + 严格依赖方向** —— 未来若要 native 化某层是局部改动
5. **Molang 是受限子集** —— 6 fn + 5 op + 2 var，软失败而不是抛异常
6. **触发 = 状态机事件** —— `(seq, id)` packed 同步字段足以，不需要 packet codec

实际工程中 #1 和 #2 是核心收益来源；#3 让动画师扩展模型不用改 Java；#4 让重构成本可控；#5 #6 是边界划得清楚的实用主义。
