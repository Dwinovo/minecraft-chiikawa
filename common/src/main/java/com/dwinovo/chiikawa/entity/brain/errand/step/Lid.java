package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/**
 * The lid of the chest a pet puts things in, as one trip of its errand leaves it: raised when the pet opens the chest and
 * lowered when it has done, or when the errand ends before it has, so no chest is left open however a delivery ends.
 * Made for one start of the errand and shared by the steps that open and close it and by the routine's own release.
 * Only a chest has a lid, with its sound and its swing; any other container is filled all the same and has none to
 * move.
 */
public final class Lid {
    private final BlockPos chest;
    private boolean raised;

    /** @param chest the container's block */
    public Lid(BlockPos chest) {
        this.chest = chest;
    }

    /** @return whether the container there has a lid to move */
    boolean exists(ServerLevel level) {
        return level.getBlockEntity(chest) instanceof ChestBlockEntity;
    }

    /** Raises the lid, with the sound of a chest opening. */
    void raise(ServerLevel level) {
        raised = true;
        move(level, SoundEvents.CHEST_OPEN, 1);
    }

    /** Lowers the lid, with the sound of a chest closing, if it was raised: safe to call at any time. */
    void lower(ServerLevel level) {
        if (!raised) {
            return;
        }
        raised = false;
        move(level, SoundEvents.CHEST_CLOSE, 0);
    }

    /** For the routine to let go of the chest as the errand ends, whether or not the pet got as far as opening it. */
    public void lowerIfRaised(Handle handle) {
        AbstractPet pet = LiveHandle.of(handle).pet();
        if (pet.level() instanceof ServerLevel level) {
            lower(level);
        }
    }

    private void move(ServerLevel level, SoundEvent sound, int open) {
        if (exists(level)) {
            level.playSound(null, chest.getX(), chest.getY(), chest.getZ(), sound, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.blockEvent(chest, level.getBlockState(chest).getBlock(), 1, open);
        }
    }
}
