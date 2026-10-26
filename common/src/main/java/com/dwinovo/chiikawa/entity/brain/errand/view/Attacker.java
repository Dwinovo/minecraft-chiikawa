package com.dwinovo.chiikawa.entity.brain.errand.view;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.PetTargeting;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

/**
 * Whoever hurt the pet lately, while the game's hurt-by memory still holds it: a creature the pet is not on the same side
 * as, that is still about. Hurt by a fall, a flame, its owner's sword or a housemate's arrow it has no attacker.
 *
 * @param id the creature's id
 * @param where where it stands
 */
public record Attacker(UUID id, GlobalPos where) {
    /**
     * @param pet the pet
     * @return who hurt it, as of the hurt-by memory that the game's own sensor keeps
     */
    public static Optional<Attacker> of(AbstractPet pet) {
        return pet.getBrain().getMemory(MemoryModuleType.HURT_BY).map(DamageSource::getEntity)
            .filter(entity -> entity instanceof LivingEntity attacker && attacker.isAlive() && attacker.level() == pet.level()
                && PetTargeting.canTarget(pet, attacker))
            .map(entity -> new Attacker(entity.getUUID(), GlobalPos.of(entity.level().dimension(), entity.blockPosition())));
    }
}
