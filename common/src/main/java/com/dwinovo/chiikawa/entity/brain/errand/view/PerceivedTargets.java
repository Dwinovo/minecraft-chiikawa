package com.dwinovo.chiikawa.entity.brain.errand.view;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.sensor.WorkSights;
import com.dwinovo.chiikawa.init.InitMemory;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

/**
 * Read-only positions of what the sensors currently remember. Sensors write these
 * whatever the directive; errands decide whether to act on them.
 *
 * <p>The blocks of work the work sensor remembers are shown only while they still are what they were
 * remembered as, as an item is only while it is lying there and a foe only while it is alive and the pet's to fight:
 * the sensors look again every second or less, and a pet that has just reaped a crop is not to set off for it once
 * more in the meantime. Of the blocks it remembers for a work the first that still holds is shown, so the next crop of
 * the field is known the moment the last is reaped and the errands that yield to harvesting are not let through in the gap.
 *
 * @param work the block of work the pet sees for each errand that is done to blocks, by the errand's id
 */
public record PerceivedTargets(
    Optional<AttackTarget> attackTarget,
    Map<ResourceLocation, GlobalPos> work,
    Optional<PickableItem> pickableItem
) {
    public static final PerceivedTargets NONE = new PerceivedTargets(Optional.empty(), Map.of(), Optional.empty());

    public PerceivedTargets {
        work = Map.copyOf(work);
    }

    public static PerceivedTargets capture(AbstractPet pet) {
        Brain<AbstractPet> brain = pet.getBrain();
        Map<ResourceLocation, GlobalPos> work = new HashMap<>();
        WorkSights.blockWorksOf(pet).forEach(block ->
            WorkSights.seenBy(pet, block).ifPresent(where -> work.put(block.errand(), where)));
        return new PerceivedTargets(
            brain.getMemory(MemoryModuleType.ATTACK_TARGET).flatMap(foe -> AttackTarget.of(pet, foe)),
            work,
            brain.getMemory(InitMemory.PICKABLE_ITEM.get()).flatMap(item -> PickableItem.of(pet, item))
        );
    }

    /** @return the block of work the pet sees for the errand */
    public Optional<GlobalPos> work(ResourceLocation errand) {
        return Optional.ofNullable(work.get(errand));
    }
}
