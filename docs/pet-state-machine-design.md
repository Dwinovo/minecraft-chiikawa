# 宠物状态机与动画决策设计

本文档描述 Chiikawa 宠物状态机的设计。它的重点不是替换 Minecraft Brain，而是在 Brain、实体同步数据和渲染管线之间建立一个清晰的动画决策层。

渲染管线侧采用 GeckoLib 风格的多 controller 模型；状态机这一侧的输出是给 controller 的 handler 看的——基础循环走 `main` controller 的 state-driven 决策，一次性事件走 `playOnce("action", ...)` / `playOnce("reaction", ...)` 触发。

## 设计目标

1. 避免把动画判断散落在 AI task、renderer 和 entity 方法里。
2. 用语义状态描述宠物，而不是直接传动画文件名。
3. 让动画师可以按照稳定命名契约交付资源。
4. 允许旧动画名继续作为 fallback 工作。
5. 让未来的心情、疲劳、亲密度、道具、乐器等机制有地方接入。
6. 保持 renderer 简单，只消费 controller handler 的产出。

## 当前边界

这一版状态机主要服务动画管线，不直接控制 AI 决策。

| 系统 | 当前职责 |
|---|---|
| Minecraft Brain | 决定宠物做什么，例如耕种、攻击、捡物品 |
| `AbstractPet` | 保存宠物模式、职业、同步触发器、背包 |
| `anim/state` | 把游戏状态转换为动画语义 |
| `PetAnimationResolver` | 把语义状态转换为基础循环候选名 |
| `ControllerInstance` | 持有每个 controller 的当前动画与 fade 状态 |
| `PetAnimator` | 容纳一只宠物所有 controller 的 mutable 状态 |
| `ChiikawaEntityRenderer` | 注册 controllers、查找动画资源、采样、渲染 |

状态机不应直接修改世界，也不应访问客户端 renderer。它应该尽量保持纯函数和小枚举。

## 核心数据结构

### `PetAnimContext`

`PetAnimContext` 是 renderer 每帧消费的状态快照：

| 字段 | 类型 | 含义 |
|---|---|---|
| `mode` | `PetMode` | 玩家选择的模式，例如 follow、sit、work |
| `job` | `PetJobRole` | 动画层看到的职业 |
| `locomotion` | `PetLocomotion` | 粗粒度移动状态 |
| `action` | `PetAction` | 当前语义动作（预留，目前总是 `NONE`） |
| `reaction` | `PetReaction` | 当前情绪反应（预留，目前总是 `NONE`） |
| `attention` | `PetAttention` | 注视目标类型 |

目前 `AbstractPet.getAnimContext(walkSpeed)` 返回基础快照：

```text
mode = getPetMode()
job = PetJobRole.fromId(getPetJobId())
locomotion = PetLocomotion.fromWalkSpeed(walkSpeed)
action = NONE
reaction = NONE
attention = NONE
```

短动作和短反应不通过每帧 context 持久表达，而是通过网络触发器进入对应 controller 的 triggered 槽。这样可以避免同一动作在每帧重复触发，也避免和基础循环抢 base controller 的状态机。

### Controller handler 的输出

`main` controller 的 handler 调 `PetAnimationResolver.resolve(ctx)`，得到一个 `List<String>` 的候选动画名（按优先级降序）。renderer 取第一个在 `AnimationLibrary` 里存在的动画。

`action` 和 `reaction` controller 默认 handler 永远返回 `null`（不主动播任何动画）。它们的动画来源完全是外部 `playOnce("action", anim)` / `playOnce("reaction", anim)` 调用——也就是 `AbstractPet` 同步触发包的客户端接收路径。

## 状态维度

### 模式：`PetMode`

这是玩家直接操作的宠物模式，目前来自实体同步数据 `PET_MODE`。

| 模式 | 动画含义 |
|---|---|
| `FOLLOW` | 默认跟随，使用 `idle/run` |
| `SIT` | 坐下，优先使用 `sit` |
| `WORK` | 工作模式，空闲时可使用职业待机 |

规则：`SIT` 优先级最高，即使 walkSpeed 非零，base loop 也优先 `sit -> idle`。

### 职业：`PetJobRole`

动画层不直接依赖 job registry，而是使用稳定枚举：

| job id | 动画职业 |
|---|---|
| `0` 或未知 | `NONE` |
| `1` | `FARMER` |
| `2` | `FENCER` |
| `3` | `ARCHER` |

工作模式下的基础循环：

| 职业 | 候选 |
|---|---|
| `FARMER` | `work_idle_farmer -> idle` |
| `FENCER` | `work_idle_fencer -> idle` |
| `ARCHER` | `work_idle_archer -> idle` |
| `NONE` | `idle` |

### 移动：`PetLocomotion`

当前保留三个 bucket：

| bucket | 触发 |
|---|---|
| `IDLE` | `walkSpeed <= 0.15` |
| `RUN` | `walkSpeed > 0.15` |
| `WALK` | 预留 |

`WALK` 暂时不会由 `fromWalkSpeed` 产出，因为当前资源主要沿用 `idle/run`。等动画师补出慢走循环后，可以把速度切成：

```text
0.00..0.05 -> IDLE
0.05..0.18 -> WALK
0.18+      -> RUN
```

### 动作：`PetAction`

动作是短时、一次性、偏"正在做事"的事件。它们通过 `AbstractPet.triggerAction` 从服务器同步到客户端，最终落到 `animator.playOnce("action", anim, nowNs)`。

| 枚举 | network id | 候选动画 |
|---|---:|---|
| `GENERIC_USE_MAINHAND` | 1 | `use_mainhand` |
| `GENERIC_SWORD_ATTACK` | 2 | `sword_attack` |
| `PICKUP` | 3 | `pickup -> use_mainhand` |
| `HARVEST` | 4 | `harvest -> use_mainhand` |
| `PLANT` | 5 | `plant -> use_mainhand` |
| `DEPOSIT` | 6 | `deposit -> use_mainhand` |
| `SLASH` | 7 | `slash -> use_mainhand` |
| `BOW_DRAW` | 8 | `bow_draw -> sword_attack` |
| `BOW_RELEASE` | 9 | `bow_release -> sword_attack` |

旧接口 `triggerAnim(String name)` 仍保留，但会转换成 `PetAction`。新代码应优先使用 `triggerAction(PetAction.X)`。

### 反应：`PetReaction`

反应是短时、一次性、偏"情绪反馈"的事件。它们通过 `AbstractPet.triggerReaction` 同步到客户端，落到 `animator.playOnce("reaction", anim, nowNs)`。

| 枚举 | network id | 候选动画 | 当前触发点 |
|---|---:|---|---|
| `HAPPY` | 1 | `happy` | 驯服成功、喂食回血 |
| `HURT` | 2 | `hurt` | 受伤音效入口 |
| `CONFUSED` | 3 | `scratch_head -> confused` | 驯服失败 |
| `REVIVE` | 4 | `revive -> happy` | 蛋糕复活完成 |

反应动画资源缺失时不会打 warning。原因是这些资源会由动画重构逐步补齐，当前版本要允许"事件已经接入但资源尚未交付"的状态。

### 注意力：`PetAttention`

注意力目前是预留维度：

| 枚举 | 预期含义 |
|---|---|
| `NONE` | 没有特殊注视目标 |
| `OWNER` | 看主人 |
| `TARGET` | 看攻击目标 |
| `BLOCK` | 看工作方块 |
| `ITEM` | 看物品 |

当前实际 head look-at 仍主要由 renderer 从实体姿态推导。未来可以让 Brain 或 sensor 在服务端/客户端产出注意力目标，再由程序化骨骼层消费。

## Resolver 规则

`PetAnimationResolver.resolve(context)` 是纯函数：

```text
PetAnimContext -> List<String>  （基础循环的候选链，第一个存在的赢）
```

当前优先级：

1. `mode == SIT`：`sit -> idle`
2. `locomotion == RUN`：`run -> walk -> idle`
3. `locomotion == WALK`：`walk -> run -> idle`
4. `mode == WORK && job != NONE`：职业工作待机
5. fallback：`idle`

resolver 只决定 base 候选名。一次性动作 / 反应的候选直接来自 `PetAction.animationCandidates()` / `PetReaction.animationCandidates()`，由触发包的客户端处理路径消费。

resolver 不做资源查询。资源查询只发生在 controller handler 或 trigger handler 中，以保持 resolver 可测试、可复用、无客户端依赖。

注意：resolver 只决定目标动画，不决定"从 A 到 B 怎么过渡"。`main` controller 的 0.16s crossfade 会自动处理 idle ↔ run、run ↔ sit 这些切换，状态机不需要列举具体边。

## 网络触发模型

短动作和短反应通过 entity data 同步：

```text
high 24 bits = sequence
low 8 bits   = event id
```

有两个独立触发器：

| 触发器 | 事件类型 | 客户端落点 |
|---|---|---|
| `ANIM_TRIGGER` | `PetAction` | `animator.playOnce("action", anim, nowNs)` |
| `REACTION_TRIGGER` | `PetReaction` | `animator.playOnce("reaction", anim, nowNs)` |

sequence 每触发一次递增。客户端只处理没见过的 sequence，因此同一个动作连续触发也能重新播放。

处理流程：

```text
server task / interact code
    -> pet.triggerAction(PetAction.X) / pet.triggerReaction(PetReaction.X)
    -> entityData.set(packed)                          ← seq +1, networkId 写低 8 位
    -> client onSyncedDataUpdated
    -> PetAction.fromNetworkId / PetReaction.fromNetworkId
    -> 候选动画名链，挨个查 AnimationLibrary
    -> 第一个找到的 BakedAnimation
    -> animator.playOnce(controllerName, anim, nowNs)
```

`controllerName` 即字符串 `"action"` 或 `"reaction"`，对应 `ChiikawaEntityRenderer` 在基类构造函数里默认注册的两个 controller。`playOnce` 把动画塞进该 controller 的 triggered 槽，覆盖其 handler 直到动画结束（或 `HOLD_ON_LAST_FRAME` 直到下次替换）。

## 现有接入点

行为动作：

| 文件 | 触发 |
|---|---|
| `HarvestCropBehavior` | `PetAction.HARVEST` |
| `PlantCropBehavior` | `PetAction.PLANT` |
| `DeliverCropBehavior` | `PetAction.DEPOSIT` |
| `PickUpItemTask` | `PetAction.PICKUP` |
| `MeleeAttackWithAnim` | `PetAction.SLASH` |
| `HurtRangedAttackTargetTask` | `PetAction.BOW_DRAW` |

反应动作：

| 文件 | 触发 |
|---|---|
| `PetInteractHandler` | `HAPPY`、`CONFUSED` |
| `AbstractPet.getHurtSound` | `HURT` |
| `PetReviveRitualManager` | `REVIVE` |

## 命名契约

给动画师的稳定动作名：

基础：

```text
idle
run
walk
sit
work_idle_farmer
work_idle_fencer
work_idle_archer
```

行为：

```text
pickup
harvest
plant
deposit
slash
bow_draw
bow_release
```

反应：

```text
happy
hurt
scratch_head
confused
revive
```

装饰循环（由 renderer 子类的 `addLoopingController` 启用）：

```text
blink           # 推荐 OVERRIDE，仅 keyframe 眼皮
breath          # 推荐 ADDITIVE，仅 keyframe 胸腹 rotation
tail_idle       # 推荐 ADDITIVE，仅 keyframe 尾骨 rotation
```

旧兼容名：

```text
use_mainhand
sword_attack
```

资源命名建议：动画资源里尽量使用语义名，fallback 名只保留给旧文件兼容。

## 如何新增一个动作

以"弹吉他"为例：

1. 在 `PetAction` 中新增 `PLAY_GUITAR(networkId, "play_guitar", "use_mainhand")`。
2. 在触发玩法的服务端逻辑里调用 `pet.triggerAction(PetAction.PLAY_GUITAR)`。
3. 动画师在每个宠物的 animation json 中添加 `play_guitar`。
4. 如果吉他是独立物品模型，优先挂到手部 locator，由骨骼动画带动。
5. 如果吉他是宠物模型的一部分，则通过动画 scale/visibility 约定处理，但这会提高资源耦合。

推荐方案：吉他作为独立 item/model，由手部 locator 控制位置和旋转。宠物动画负责手臂、身体、头部节奏，物品跟随 locator。

注意：弹吉他是一次性 trigger（即使时长几秒），它走 `action` controller 的 playOnce 路径。如果想做"持续弹吉他"的长期状态，把 `PLAY_GUITAR` 也加到 `PetMode` 或一个新的状态字段，然后在 `PetAnimationResolver` 里把它折进 base 候选链。

## 如何新增一个长期状态

以"疲劳"为例：

1. 新增枚举或数值状态，例如 `PetMood` 或 `PetEnergyLevel`。
2. 把状态存到实体数据或持久化 NBT。
3. 在 `getAnimContext` 中填入快照。
4. 在 `PetAnimationResolver` 中决定它是否影响 base loop（例如新增 `idle_tired` 候选）。
5. 如果它只影响尾巴、耳朵、头部等细节，优先交给 `BoneInterceptor`。
6. 如果它需要单独叠一层装饰循环（"心情低落时呼吸幅度加大"），可以注册一个独立的 ADDITIVE controller，handler 根据状态返回不同的 BakedAnimation 或 `null`。

判断规则：

| 状态影响 | 推荐实现 |
|---|---|
| 看向某个目标 | 程序化骨骼（`BoneInterceptor` LOOK_AT 阶段） |
| 耳朵/尾巴轻微情绪 | 程序化骨骼，或一个 ADDITIVE controller |
| 完整姿态变化 | base loop（main controller 的 resolver 候选） |
| 一次性表达 | playOnce 触发（action/reaction controller） |
| 需要服务端同步 | entity data 或持久化数据 |

## 与 Minecraft Brain 的关系

不要把 Brain 替换成自研状态机。Brain 很适合做 AI 行为选择，状态机只负责把行为结果翻译成动画语义。

推荐分层：

```text
Sensor/Memory
    -> Brain task chooses behavior
    -> task mutates world or entity
    -> task emits PetAction/PetReaction
    -> animation resolver / playOnce → controller
```

这样每个 task 只需要在关键时刻发出语义事件，不需要知道 animation json 的细节，更不需要知道 controller 列表。

## 未来阶段

### 阶段 6：动画资源契约落地

1. 给动画师确认动作名单。
2. 在数据生成或资源检查里验证每个宠物是否缺少关键动画。
3. 为缺失资源输出清晰 warning。

### 阶段 7：长期宠物状态

候选状态：

| 状态 | 类型 | 用途 |
|---|---|---|
| 心情 | enum/数值 | 影响 idle、尾巴、耳朵 |
| 疲劳 | 数值 | 影响速度和 idle 姿态 |
| 亲密度 | 数值 | 影响 happy、跟随距离、互动 |
| 饥饿 | 数值 | 影响工作积极性 |
| 战斗压力 | enum | 影响攻击待机和受伤反应 |

这些状态应先进入实体持久化和同步，再由 `PetAnimContext` 暴露给 controller handler。

### 阶段 8：装饰 controller 与混合策略

当前已经支持 OVERRIDE / ADDITIVE 两种 blend mode。后续装饰需求往往可以通过新增 ADDITIVE controller 解决：

- "心情好尾巴摆动幅度变大"——一个 ADDITIVE controller，handler 根据 `ctx.mood` 选不同的 `tail_idle_*` 动画
- "受伤后呼吸急促"——一个 ADDITIVE controller，handler 根据是否在 hurt 状态选 `breath_fast` 或 `breath`
- "持物时上半身姿态"——一个 OVERRIDE controller 注册在 main 之后、blink 之前

如果将来出现"骨骼 mask"（同一动画只影响指定子树）这种 GeckoLib 没有的需求，再考虑在 `ControllerConfig` 上加 mask 字段或在 `PoseSampler.sample` 入口过滤 channel。目前没有信号触发这条改动。

## 维护原则

1. 业务代码只触发语义事件，不直接查动画资源。
2. resolver 必须保持纯函数。
3. controller handler 必须幂等——同一个动画连续返回不重启计时。
4. renderer 不写职业、任务、心情判断。
5. 新动画必须有 fallback 或明确说明无 fallback。
6. 网络 id 一旦发布，避免重排。
7. 能用程序化骨骼处理的过渡，不强迫动画师重复制作。
8. 能用独立模型和 locator 挂载的道具，不塞进每个宠物主模型。
9. 装饰循环优先用 ADDITIVE 叠加在 main 之上，不要让 main 包揽所有微动。
