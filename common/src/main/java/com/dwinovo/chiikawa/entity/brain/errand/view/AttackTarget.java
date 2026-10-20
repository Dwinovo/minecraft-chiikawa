package com.dwinovo.chiikawa.entity.brain.errand.view;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.sensor.PetAttackbleEntitySensor;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.LivingEntity;

/**
 * What the attack sensor has named for a pet to fight, as it is now: which creature, and where. One that is gone,
 * that the pet may not attack, or that is out of the sensor's range, is not shown, whatever the sensor still remembers:
 * it looks again only every half second, and a pet that has just put a foe down is not to turn on its corpse in the
 * meantime, nor one that has been taken far away from its foe to go back to the place it was taken from.
 *
 * @param id the creature's id, which is what a fight keeps to
 * @param where where it stands
 */
public record AttackTarget(UUID id, GlobalPos where) {
    /**
     * @param pet who would fight it
     * @param foe what the sensor named
     * @return it as that pet's to fight, or nothing if it is gone, out of range or is on the pet's own side
     */
    public static Optional<AttackTarget> of(AbstractPet pet, LivingEntity foe) {
        if (!PetAttackbleEntitySensor.fightable(pet, foe)) {
            return Optional.empty();
        }
        return Optional.of(new AttackTarget(foe.getUUID(), GlobalPos.of(foe.level().dimension(), foe.blockPosition())));
    }
}
