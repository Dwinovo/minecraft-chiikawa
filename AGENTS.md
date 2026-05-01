# Chiikawa 项目索引

这是给未来 agents 和协作者看的短索引。详细设计都放在 `docs/` 下面。

## 当前上下文

- 当前迁移目标：Minecraft `26.1.2`。
- 宠物实体动画已经移除 Geckolib 运行时依赖。
- 宠物渲染现在由仓库内自研 Bedrock 模型/动画管线驱动。
- 宠物动画决策正在迁移到语义状态机，避免散落的动画名调用。

## 设计文档

- `docs/rendering-pipeline-design.md`
  - 自研 Bedrock 渲染与动画管线的端到端设计。
  - 覆盖资源加载、烘焙、pose 采样、channel 分层、物品挂载、程序化骨骼和资源命名。
- `docs/pet-state-machine-design.md`
  - 宠物动画状态机的详细设计。
  - 覆盖 `PetAnimContext`、`PetAnimationResolver`、`PetAction`、`PetReaction`、网络触发器、当前接入点和未来长期状态。
- `docs/animation-pipeline.md`
  - 既有管线实现细节和历史背景。
  - 调试坐标变换、Molang、pose buffer、GUI 双 extract 计时问题时优先看它。

## 关键代码区域

- `common/src/main/java/com/dwinovo/chiikawa/anim/`
  - 自研模型、动画、Molang、baked 数据、runtime、state 和 renderer。
- `common/src/main/java/com/dwinovo/chiikawa/entity/AbstractPet.java`
  - 宠物 mode/job 数据、action/reaction 网络触发器、pet animator 入口。
- `common/src/main/java/com/dwinovo/chiikawa/entity/brain/task/`
  - Brain task 接入点，负责发出语义 `PetAction`。
- `common/src/test/java/com/dwinovo/chiikawa/anim/`
  - resolver 和 animator 的单元测试。

## 验证命令

常用命令：

```powershell
.\gradlew.bat :common:test --console=plain
.\gradlew.bat build --console=plain
.\gradlew.bat :neoforge:runClient
.\gradlew.bat :neoforge:runData
```

来自 `net.rubygrapefruit.platform` 的 Gradle/JDK restricted native access warning 当前不影响构建。
