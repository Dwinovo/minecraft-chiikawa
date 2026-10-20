package com.dwinovo.chiikawa.entity.brain.errand.view;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.sensor.FarmTarget;
import com.dwinovo.chiikawa.init.InitMemory;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

/**
 * Read-only positions of what the sensors currently remember. Sensors write these
 * whatever the directive; errands decide whether to act on them.
 *
 * <p>The blocks of work the farmer sensor remembers are shown only while they still are what they were
 * remembered as, as an item is only while it is lying there and a foe only while it is alive and the pet's to fight:
 * the sensors look again every second or less, and a pet that has just reaped a crop is not to set off for it once
 * more in the meantime.
 */
public record PerceivedTargets(
    Optional<AttackTarget> attackTarget,
    Optional<GlobalPos> harvest,
    Optional<GlobalPos> plant,
    Optional<GlobalPos> container,
    Optional<GlobalPos> weed,
    Optional<GlobalPos> mushroom,
    Optional<PickableItem> pickableItem
) {
    public static final PerceivedTargets NONE = new PerceivedTargets(Optional.empty(), Optional.empty(),
        Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

    public static PerceivedTargets capture(AbstractPet pet) {
        Brain<AbstractPet> brain = pet.getBrain();
        return new PerceivedTargets(
            brain.getMemory(MemoryModuleType.ATTACK_TARGET).flatMap(foe -> AttackTarget.of(pet, foe)),
            FarmTarget.HARVEST.seenBy(pet),
            FarmTarget.PLANT.seenBy(pet),
            FarmTarget.DELIVER.seenBy(pet),
            FarmTarget.WEED.seenBy(pet),
            FarmTarget.MUSHROOM.seenBy(pet),
            brain.getMemory(InitMemory.PICKABLE_ITEM.get()).flatMap(item -> PickableItem.of(pet, item))
        );
    }
}
