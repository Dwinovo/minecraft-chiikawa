package com.dwinovo.chiikawa.entity.brain.constraint;

import java.util.OptionalDouble;
import net.minecraft.core.GlobalPos;

/**
 * The area a pet is allowed to act in, derived by {@link PetConstraints#anchorOf}.
 *
 * <p>Intents only start on targets within {@code reach}; while acting the pet may
 * go as far as {@code leash}, past which the anchor leash walks it back. The gap
 * between the two is the pursuit margin that keeps a pet from flip-flopping
 * between chasing and being pulled back. Distances are measured between block
 * positions.
 *
 * @param center the anchor point
 * @param reach radius in which intents may pick targets
 * @param leash radius past which the pet is walked back to {@code center};
 *              {@link Double#POSITIVE_INFINITY} when nothing pulls it back
 * @param teleport distance past which the pet is teleported instead; only present
 *                 while the anchor follows the owner
 * @param canMove whether the pet may move at all
 */
public record PetAnchor(GlobalPos center, double reach, double leash, OptionalDouble teleport, boolean canMove) {
    public static final double UNLEASHED = Double.POSITIVE_INFINITY;

    public boolean withinReach(GlobalPos pos) {
        return within(pos, reach);
    }

    public boolean withinLeash(GlobalPos pos) {
        return within(pos, leash);
    }

    /**
     * @param pos the pet's position
     * @return whether the pet is far enough from the owner-following center to be teleported
     */
    public boolean beyondTeleport(GlobalPos pos) {
        return teleport.isPresent() && !within(pos, teleport.getAsDouble());
    }

    private boolean within(GlobalPos pos, double radius) {
        return pos.dimension().equals(center.dimension())
            && pos.pos().distSqr(center.pos()) < radius * radius;
    }
}
