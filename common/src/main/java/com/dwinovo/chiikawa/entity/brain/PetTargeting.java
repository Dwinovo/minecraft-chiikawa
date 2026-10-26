package com.dwinovo.chiikawa.entity.brain;

import java.util.Objects;
import java.util.UUID;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Single source of truth for which entities a pet may pick or keep as its
 * {@code ATTACK_TARGET}.
 *
 * <p>A pet never fights its own side: itself, its owner, any player, any
 * ownable entity (other pets, wolves, cats, horses…) that belongs to the same
 * owner, or anything the game counts as its ally, such as what is in its owner's
 * team. This matters most for retaliation: {@code HURT_BY_ENTITY} is set
 * whenever something damages the pet, including the owner's sword sweep or a
 * stray arrow from a sibling archer pet.
 */
public final class PetTargeting {
    private PetTargeting() {
    }

    /**
     * @param pet the attacking pet
     * @param target a candidate or current attack target, may be {@code null}
     * @return whether {@code pet} is allowed to attack {@code target}
     */
    public static boolean canTarget(TamableAnimal pet, @Nullable LivingEntity target) {
        if (target == null) {
            return false;
        }
        UUID targetOwner = target instanceof OwnableEntity ownable ? ownable.getOwnerUUID() : null;
        return !isProtected(
            target == pet,
            target instanceof Player,
            pet.isAlliedTo(target),
            pet.getOwnerUUID(),
            target.getUUID(),
            targetOwner
        );
    }

    /**
     * Pure form of the targeting rule, kept free of live entities so it can be
     * unit tested.
     *
     * @param isSelf the target is the pet itself
     * @param isPlayer the target is a player
     * @param isAllied the pet and the target are on one side, as the game counts it: one team, or an owner's pet
     * @param petOwner the pet's owner UUID, or {@code null} if untamed
     * @param targetId the target's UUID
     * @param targetOwner the target's owner UUID if it is ownable, else {@code null}
     * @return whether the target must never be attacked by the pet
     */
    static boolean isProtected(boolean isSelf, boolean isPlayer, boolean isAllied, @Nullable UUID petOwner,
            UUID targetId, @Nullable UUID targetOwner) {
        if (isSelf || isPlayer || isAllied) {
            return true;
        }
        if (petOwner == null) {
            return false;
        }
        return petOwner.equals(targetId) || Objects.equals(petOwner, targetOwner);
    }
}
