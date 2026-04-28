# 自有 Bedrock 动画管线

替代 GeckoLib 的渲染/动画管线，处理 7 只 chiikawa 宠物的所有动画播放、骨骼程序化干预、手持物挂载和服务器触发同步。

## 触发原因

GeckoLib 5.4 在 1.21.11 下有系统性 bug — [bernie-g/geckolib#848](https://github.com/bernie-g/geckolib/issues/848)。`AnimationController.lastAnimatableAge` 是可变累积时间，每次 `extractRenderState` 调用都会前进。`InventoryScreen.renderEntityInInventoryFollowsMouse` 在 GUI 预览实体时每帧多触发一次 extract，结果是动画约 2 倍速 + 移动时抖动。

主流 mod 里没有"GeckoLib 实体 + 容器 GUI 实时预览"的先例，所有 workaround 都强耦合 GeckoLib 内部。所以我们做了自己的管线。

## 架构

七层，依赖箭头从左到右：

```
format → compile → baked → runtime → render → api
 POJO    bake-time  immut    per-      MC       外部
                  shared   entity   integration  接口
```

每层只依赖左侧。`format`/`compile` 仅在资源加载时跑；`baked` 是不可变共享数据；`runtime` 是每实体一份；`render` 是 Minecraft 集成；`api` 是业务代码看到的入口。

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
│   └── BedrockResourceLoader.java#   ResourceManagerReloadListener，挂在两个 client 入口
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
│   ├── BakedAnimation.java       #   name, duration, loop, channels[]
│   └── BakedBoneChannel.java     #   boneIdx, type, times[], values[], lerpModes[], molangSlots[]
│
├── runtime/                      # 第 5 层：每实体一份的可变状态
│   ├── PetAnimator.java          #   4 个 channel 槽
│   ├── AnimationChannel.java     #   record(BakedAnimation, startTimeNs, looping) — 不可变
│   └── PoseSampler.java          #   纯函数：(channel, nowNs, ctx) → poseBuf
│
├── render/                       # 第 6 层：Minecraft 集成
│   ├── ChiikawaEntityRenderer.java # extends EntityRenderer<T, ChiikawaRenderState>
│   ├── ChiikawaRenderState.java    # 携带 modelKey/texture/channel snapshot/heldItem
│   ├── ModelRenderer.java          # 骨骼 DAG 遍历 + cube quad 发射
│   ├── BoneInterceptor.java        # 骨骼程序化覆写接口
│   ├── PetBoneInterceptor.java     # 默认实现（头/耳/尾）
│   ├── BoneAttachmentLayer.java    # 物品挂载到 RightHandLocator
│   └── impl/                       # 7 个 trivial 子类（ChiikawaRenderer 等）
│
└── api/                          # 第 7 层：业务接口
    ├── ModelLibrary.java         #   namespace:path → BakedModel 注册表
    ├── AnimationLibrary.java     #   namespace:path/anim → BakedAnimation 注册表
    └── ChiikawaAnimated.java     #   实体侧接口：getPetAnimator() + getMainAnimationName()
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
BakedModel + BakedAnimation + MolangNode AST
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
    └─ 调 ChiikawaAnimated.getMainAnimationName(walkSpeed) → setMain(idle/run/sit, looping=true)
    │
    ▼
ChiikawaEntityRenderer.submit
    │
    ├─ 分配 poseBuf = float[boneCount * 9]
    ├─ PoseSampler.resetIdentity(poseBuf)
    ├─ 填 MolangContext.vars (ground_speed)
    ├─ PoseSampler.sample(mainChannel, nowNs, ctx, poseBuf)
    ├─ for each subChannel: PoseSampler.sample(...)
    ├─ for each interceptor: interceptor.apply(model, state, ctx, poseBuf)  [程序化覆写]
    │
    ├─ poseStack.rotateY(180 - bodyRot) + scale(1/16)
    ├─ collector.submitCustomGeometry(deferred ModelRenderer.render call)
    └─ heldItemLayer.submit(model, poseBuf, "RightHandLocator", ...)
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

### 采样是纯函数（GeckoLib #848 的根本修复）

```java
PoseSampler.sample(channel, nanoTime(), ctx, poseBuf)
```

是引用透明的：相同输入永远相同输出，**没有任何累积时间状态**。`AnimationChannel` 是 `record(BakedAnimation, long startTimeNs, boolean looping)`。每次采样都从 `nowNs - startTimeNs` 重新算，不存"上次走到哪了"。

InventoryScreen 在同一帧调两次 extract，两次拿到几乎相同的 `nanoTime()`，采样输出 bit-identical → 物理性消除 #848。

### Channel 层级

`PetAnimator.CHANNEL_COUNT = 4`，但当前仅用：

| layer | 用途 |
|---|---|
| 0 | main loop（idle / run / sit），`setMain` 维护，幂等 |
| 1 | trigger 一次性动画（use_mainhand、sword_attack），`trigger()` 写入 |
| 2-3 | 保留（多层加性混合的扩展点，未实现）|

Trigger channel 的 `looping=false`，`PoseSampler` 在 `t >= duration` 时 clamp 到末尾值（不会自动清除，但视觉上停在末尾 pose；下一次 `trigger()` 替换整个 record，等于重新开始）。

### MolangContext 范围

只有两个 slot 真有值：

| slot | 来源 |
|---|---|
| `query.anim_time` / `q.anim_time` | `PoseSampler.sample` 每个 channel 采样前填，等于该 channel 的本地时间 |
| `query.ground_speed` | `ChiikawaEntityRenderer.submit` 每帧从 `walkAnimation.speed` 填 |

**故意不暴露的**：

| Molang 引用 | 状态 | 替代方案 |
|---|---|---|
| `ysm.head_yaw` / `ysm.head_pitch` | 软失败 → `Const(0)` | `PetBoneInterceptor` 程序化控制 AllHead 骨骼 |
| `v.L6_P0` / `v.L4_P0` / `v.L6_P00` | 软失败 → `Const(0)` | 这些是 Blockbench IK 导出残留，没有 SET 站点 |

详见 [`MolangContext`](../common/src/main/java/com/dwinovo/chiikawa/anim/molang/MolangContext.java) 顶部注释。

`MolangCompiler` 对未知变量/函数 `warn` 一次然后返回 `Const(0)`，不抛异常 —— 一个表达式坏不影响整个动画加载。

### 单位约定

PoseStack 在我们的渲染流水线里始终处于 **1/16-scaled pixel 空间**：
- `scale(1/16)` 已在 `submit` 阶段应用
- 骨骼 pivot 用原始 pixel 数值，可直接 `translate(pivotX)`
- ModelRenderer 顶点也直接用原始 pixel 数值

但 **物品** 在 Mojang 的 API 里使用块单位（BakedQuad vertex 是 0..1 块，display transform 也是块为单位）。所以 `BoneAttachmentLayer.submit` 在 chain walk 终点会 `scale(16, 16, 16)` 抵消我们的 1/16，恢复块单位 → 物品才能以正常尺寸渲染。

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

[`PetBoneInterceptor`](../common/src/main/java/com/dwinovo/chiikawa/anim/render/PetBoneInterceptor.java) 默认实现按名查找：

| 骨骼名 | 用途 | 缺失时 |
|---|---|---|
| `AllHead` | 头部 yaw/pitch 跟随玩家 | 跳过，pet 头不动 |
| `LeftEar` / `RightEar` | 闲置摆动 + 跑动后压 | 跳过，耳朵静止 |
| `tail` | Y 轴轻微摇摆 | 跳过，尾巴静止 |
| `RightHandLocator` | 手持物挂载锚点 | 物品不显示 |

模型缺这些骨骼**不会崩**，只是对应的程序化效果失效。

### 必须存在的动画名

[`AbstractPet.getMainAnimationName`](../common/src/main/java/com/dwinovo/chiikawa/entity/AbstractPet.java) 按状态选：

| 动画名 | 何时播放 |
|---|---|
| `idle` | 默认状态（也是 fallback） |
| `run` | walkSpeed > 0.15 |
| `sit` | PetMode == SIT |
| `use_mainhand` | trigger（farmer / fencer 攻击）|
| `sword_attack` | trigger（archer 攻击） |

缺动画时 `setMain` 会跳过（没有 fallback 到 idle 的话主循环就停在上一个动画）。

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
3. 在 `ChiikawaEntityRenderer.submit` 或 `PoseSampler.sample` 适当位置填 `ctx.vars[SLOT_FOO] = ...`

### 添加新触发动画

1. 在 `AbstractPet.java` 加 `TRIGGER_FOO = N` 常量（占低 8 位，0 是保留）
2. `animIdFor` / `animNameFor` 两个 switch 各加 case，name 必须与动画文件中的名字一致
3. server 端逻辑里调 `pet.triggerAnim("foo")`
4. 重新打包后客户端会自动响应（`onSyncedDataUpdated` 已经处理了）

### 添加新 BoneInterceptor

如果需要新的程序化骨骼覆写（比如尾巴随心情摆动幅度变化）：

1. 实现 [`BoneInterceptor`](../common/src/main/java/com/dwinovo/chiikawa/anim/render/BoneInterceptor.java) 接口（`@FunctionalInterface`，一个方法）
2. 在 `ChiikawaEntityRenderer` 的 `interceptors` 数组加进去（按调用顺序排）
3. 多个 interceptor 顺序执行，**后写覆盖前写**，所以放在数组靠后的优先级更高

### 修改 main loop 状态机

[`AbstractPet.getMainAnimationName(walkSpeed)`](../common/src/main/java/com/dwinovo/chiikawa/entity/AbstractPet.java) — 直接改这个方法。返回的字符串就是动画名。renderer 每帧调一次，配合 `PetAnimator.setMain` 的幂等性自动切换。

## 常见坑

记录的是**已经踩过、改过、值得后人警惕**的具体场景。

### GUI extract 之后 InventoryScreen 会覆写 state.bodyRot/yRot/xRot

`InventoryScreen.renderEntityInInventoryFollowsMouse` 调用顺序：
1. `renderer.createRenderState(entity, partialTick)` —— 我们的 extract 跑完
2. **直接覆写** `state.bodyRot = 180 + f*20`、`state.yRot = f*20`、`state.xRot = -g*20`（鼠标驱动）

这意味着 `state.yRot - state.bodyRot = -180`（永远）。任何在 submit 阶段从 state 字段反推 entity 真实状态的代码都会拿到错值。

**修法**：所有派生量在 extract 阶段就 snapshot 到 state 自己的字段（如 `state.netHeadYaw`、`state.headPitch`），submit 阶段读 snapshot。等价于 GeckoLib 用 DataTicket 暂存。

### `ysm.head_yaw` / `ysm.head_pitch` 是 vestigial

动画文件可能引用 `ysm.head_yaw`（来自 Yes Steve Model mod 的命名空间），但 GeckoLib 也从来没真填过这个变量。头部跟随**一直是渲染器程序化覆写** AllHead 骨骼实现的，不是 Molang。

**如果不小心填了真值**，跑动 + GUI 鼠标就会让 head_yaw 跑到 ±180，配合 `Root.rotZ = 0.4*ysm.head_yaw` 这种动画表达式会让整只宠物侧躺 72°。

**修法**：`MolangContext` 里**不要**给 ysm.* 加 slot。让 `MolangCompiler` 软失败成 `Const(0)`。头/耳/尾交给 `PetBoneInterceptor`。

### 物品在 1/16-scaled PoseStack 里直接 submit 会变 5cm 小

我们的 PoseStack 是 1/16 scale 空间（pixel 单位），但 `ItemStackRenderState.submit` 内部 vertex 是 0..1 块单位。直接 submit 物品会被额外乘 1/16 → 0.85 块的剑变成 5cm。

**修法**：`BoneAttachmentLayer` 在 chain walk 终点 `scale(16, 16, 16)` 抵消。

### 浮点 identity 比较用 `== 0f`

[`AnimationBaker.isConstantIdentity`](../common/src/main/java/com/dwinovo/chiikawa/anim/compile/AnimationBaker.java) 和 [`ModelRenderer.renderBone`](../common/src/main/java/com/dwinovo/chiikawa/anim/render/ModelRenderer.java) 都用精确等于 `== 0f` / `== 1f` 判断 identity。如果未来出现"动画文件里写的是 `1e-10`，浮点不精确等于 0"导致剪枝失败，可以加个 epsilon（比如 `Math.abs(x) < 1e-6`）。

目前 Blockbench 导出的常量值都是规整的 `0` 或 `1`，没问题。

### 多实体共享 renderer 实例 + deferred submitCustomGeometry lambda

`submitCustomGeometry` 的 lambda 不在 submit 调用时执行，而是延迟到 batch 渲染。如果多个 entity 共享 renderer 实例，把 pose buffer 缓存在 renderer 上会被后一个 entity 的 submit 覆写，前一个 lambda 跑时拿到错的数据。

**修法**：每次 submit **新分配** pose buffer。代价是 ~864 字节/调用 × 几千次/秒 ≈ 几 MB/秒 GC 压力，可接受。

## 性能现状

参考 `git log` 的最后一个 `perf(anim):` 提交。当前优化：

1. **AnimationBaker bake 时丢弃 identity 通道**（all-zero rotation/position、all-one scale、所有 keyframe 都是 identity 的）
2. **ModelRenderer 跳过 identity 骨骼的 push/pop + pivot 三明治**（24 骨骼里大概 18 个是组织性的）
3. **BoneAttachmentLayer chain walk 应用同样的 fast path**

热路径主要时间花在 Mojang 的 `BufferBuilder.addVertex`，我们的 Java 计算占整体 5-10%。Rust JNI 化 sampling 实测**会更慢**（boundary overhead 远超优化收益），所以不做。

下一批候选优化（按 ROI 从高到低，未实施）：

| 优化 | 预期收益 | 复杂度 |
|---|---|---|
| 距离 LOD（远处宠物每 N 帧采样一次 + 缓存 pose） | 远观大量 pet 时 N× | 中 |
| Bake 时折叠 no-op 骨骼（`MAllBody/AllBody/...` 上提到父） | 模型层级压扁，省 push/pop | 中 |
| Crossfade 动画切换（idle ↔ run 5 tick 淡入） | 视觉连贯（这是功能不是性能，但会增加 sample 成本） | 中 |

不要为了优化而优化 —— 当前没有性能信号触发以上改动。

## 关键设计原则（事后回看）

1. **采样是纯函数** —— GeckoLib #848 类问题在新管线物理上不可能发生
2. **数据导向布局**（DOD/SoA） —— Java JIT 也喜欢，不依赖 Rust 化也比传统 OO 风格快 2-3 倍
3. **分层架构 + 严格依赖方向** —— 未来若要 native 化某层是局部改动
4. **Molang 是受限子集** —— 6 fn + 5 op + 2 var，软失败而不是抛异常
5. **触发 = 状态机事件** —— `(seq, id)` packed 同步字段足以，不需要 packet codec

实际工程中 #1 和 #2 是核心收益来源；#3 让重构成本可控；#4 #5 是边界划得清楚的实用主义。
