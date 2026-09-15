package com.dwinovo.chiikawa.entity.brain.intent;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitMemory;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.Level;

/**
 * Read-only positions of what the sensors currently remember. Sensors write these
 * whatever the directive; intents decide whether to act on them.
 */
public record PerceivedTargets(
    Optional<GlobalPos> attackTarget,
    Optional<GlobalPos> harvest,
    Optional<GlobalPos> plant,
    Optional<GlobalPos> container,
    Optional<GlobalPos> pickableItem
) {
    public static final PerceivedTargets NONE = new PerceivedTargets(
        Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

    static PerceivedTargets capture(AbstractPet pet) {
        Brain<AbstractPet> brain = pet.getBrain();
        ResourceKey<Level> dimension = pet.level().dimension();
        return new PerceivedTargets(
            brain.getMemory(MemoryModuleType.ATTACK_TARGET)
                .map(target -> GlobalPos.of(target.level().dimension(), target.blockPosition())),
            brain.getMemory(InitMemory.HARVEST_POS.get()).map(pos -> GlobalPos.of(dimension, pos)),
            brain.getMemory(InitMemory.PLANT_POS.get()).map(pos -> GlobalPos.of(dimension, pos)),
            brain.getMemory(InitMemory.CONTAINER_POS.get()).map(pos -> GlobalPos.of(dimension, pos)),
            brain.getMemory(InitMemory.PICKABLE_ITEM.get()).map(item -> GlobalPos.of(dimension, item.blockPosition()))
        );
    }
}
