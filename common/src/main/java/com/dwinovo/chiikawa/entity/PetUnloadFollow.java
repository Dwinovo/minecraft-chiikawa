package com.dwinovo.chiikawa.entity;

import com.dwinovo.chiikawa.entity.brain.constraint.PetConstraints;
import com.dwinovo.chiikawa.entity.brain.constraint.PetOwnership;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.entity.EntityAccess;

/**
 * Rescues following pets whose chunk is about to unload.
 *
 * <p>{@link com.dwinovo.chiikawa.entity.brain.task.tameable.AnchorLeashBehavior} only
 * teleports a pet from its own AI, so an owner who jumps far away (commands, ender
 * pearls, fast elytra flight, respawning) can leave the pet's chunk to stop ticking
 * and unload before that check runs. The entity manager mixin calls
 * {@link #onChunkPreUnload} with the entities of each chunk right before they are
 * saved and unloaded, and eligible pets are moved next to their owner instead.
 *
 * <p>Technique from Snownee's Companion mod, as used by TouhouLittleMaid.
 */
public final class PetUnloadFollow {
    private PetUnloadFollow() {
    }

    /**
     * Teleports every eligible pet among {@code entities} to its owner. Pets for
     * which no safe spot is found are left alone and unload normally.
     *
     * @param entities a snapshot of the entities in a chunk that is about to unload
     */
    public static void onChunkPreUnload(List<? extends EntityAccess> entities) {
        for (EntityAccess entity : entities) {
            if (entity instanceof AbstractPet pet && pet.level() instanceof ServerLevel level) {
                LivingEntity owner = findFollowedOwner(pet);
                if (owner != null) {
                    pet.teleportToOwner(level, owner);
                }
            }
        }
    }

    /**
     * A pet follows its owner out of the chunk exactly when its anchor follows the
     * owner: tamed, told to follow, not leashed or riding, and the owner online,
     * alive, not spectating and in the same level.
     *
     * @return the owner this pet should follow out of an unloading chunk, or
     *         {@code null} if the pet should stay (and unload) where it is
     */
    private static LivingEntity findFollowedOwner(AbstractPet pet) {
        if (!pet.isAlive() || !PetConstraints.anchorOf(pet, PetOwnership.of(pet)).followsOwner()) {
            return null;
        }
        return pet.getOwner();
    }
}
