package com.dwinovo.chiikawa.entity;

import java.util.List;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.entity.EntityAccess;

/**
 * Rescues following pets whose chunk is about to unload.
 *
 * <p>{@link com.dwinovo.chiikawa.entity.brain.task.tameable.KeepAroundBehavior} only
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
                ServerPlayer owner = findFollowedOwner(pet, level);
                if (owner != null) {
                    pet.teleportToOwner(level, owner);
                }
            }
        }
    }

    /**
     * @return the online owner this pet should follow out of an unloading chunk, or
     *         {@code null} if the pet should stay (and unload) where it is
     */
    private static ServerPlayer findFollowedOwner(AbstractPet pet, ServerLevel level) {
        if (!pet.isAlive() || !pet.isTame() || pet.getPetDirective() != PetDirective.FOLLOW
                || pet.isLeashed() || pet.isPassenger()) {
            return null;
        }
        UUID ownerId = pet.getOwnerUUID();
        MinecraftServer server = level.getServer();
        if (ownerId == null || server == null) {
            return null;
        }
        ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
        if (owner == null || !owner.isAlive() || owner.isSpectator()) {
            return null;
        }
        // No cross-dimension following.
        return owner.level() == level ? owner : null;
    }
}
