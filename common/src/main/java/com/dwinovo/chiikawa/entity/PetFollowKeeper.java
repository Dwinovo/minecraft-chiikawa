package com.dwinovo.chiikawa.entity;

import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.entity.brain.constraint.PetConstraints;
import com.dwinovo.chiikawa.entity.brain.constraint.PetOwnership;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.entity.EntityTypeTest;

/**
 * Fetches following pets that have been left behind. The server looks for them rather
 * than the pets looking for their owner, and that is the whole point: a pet only checks
 * on itself while it is thinking, and a pet in a chunk that is loaded but not ticking —
 * another player's view distance beyond simulation distance, the edge of the spawn
 * chunks, a chunk loader's ring — is not thinking at all. Such a pet used to stay where
 * it was while its owner walked away, and nothing was ever going to notice.
 *
 * <p>This is the one place a pet is teleported to catch up. The pre-unload hook
 * ({@link PetUnloadFollow}) stays as a last resort for the moment a chunk actually goes,
 * which can happen between two checks.
 */
public final class PetFollowKeeper {
    /** How often the server looks. Two seconds is quicker than anyone walks out of sight. */
    private static final int CHECK_TICKS = 40;

    private PetFollowKeeper() {
    }

    /** Registered by both loaders on the server tick. */
    public static void tickServer(MinecraftServer server) {
        if (server.getTickCount() % CHECK_TICKS != 0) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            // No owner in a level means nobody to have fallen behind in it.
            if (!level.players().isEmpty()) {
                fetchStrays(level);
            }
        }
    }

    /** Teleports every pet in {@code level} that has fallen behind the owner it follows. */
    public static void fetchStrays(ServerLevel level) {
        for (AbstractPet pet : level.getEntities(EntityTypeTest.forClass(AbstractPet.class),
                PetFollowKeeper::leftBehind)) {
            LivingEntity owner = pet.getOwner();
            if (owner != null) {
                pet.teleportToOwner(level, owner);
            }
        }
    }

    /**
     * Whether this pet has fallen far enough behind to be fetched: it is following an
     * owner who is here in this level, it is free to move, and it is past the distance
     * its anchor allows. A pet that is sitting, leashed, riding or wild is nobody's to
     * fetch, and an owner in another dimension is out of reach of a plain teleport.
     */
    private static boolean leftBehind(AbstractPet pet) {
        if (!pet.isAlive() || pet.isPassenger()) {
            return false;
        }
        LivingEntity owner = pet.getOwner();
        if (owner == null || owner.level() != pet.level()) {
            return false;
        }
        PetAnchor anchor = PetConstraints.anchorOf(pet, PetOwnership.of(pet));
        return anchor.canMove()
            && anchor.beyondTeleport(GlobalPos.of(pet.level().dimension(), pet.blockPosition()));
    }
}
