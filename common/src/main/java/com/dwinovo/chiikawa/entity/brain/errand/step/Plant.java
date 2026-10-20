package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.anim.state.PetAction;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import com.dwinovo.chiikawa.utils.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * Plants a seed from the pet's bag on the soil at a place. The seed is the one that fits this soil and not just the first
 * the pet holds, so nether wart is planted on soul sand even when crop seeds are in the bag too, and one seed is used
 * up. Gives up if the pet has no seed for this soil any more, or the seed could not be put in because the space above
 * is taken. Done in the tick it takes, so it is never cut half way: the seed is in the ground or still in the bag. It
 * does not look how near the pet is; the walk before it is what brings the pet within reach.
 */
public final class Plant implements Step {
    private final BlockPos soil;

    /** @param soil the block the seed is planted on, which is the soil and not the plant */
    public Plant(BlockPos soil) {
        this.soil = soil;
    }

    @Override
    public void begin(Handle handle) {
    }

    @Override
    public StepStatus tick(Handle handle) {
        AbstractPet pet = LiveHandle.of(handle).pet();
        if (!(pet.level() instanceof ServerLevel level)) {
            return StepStatus.gaveUp("gone");
        }
        ItemStack seed = Utils.getSeedForBase(level, pet, soil);
        if (seed.isEmpty()) {
            return StepStatus.gaveUp("no_seed");
        }
        if (!Utils.plantSeed(level, pet, soil, seed)) {
            return StepStatus.gaveUp("cannot_plant");
        }
        handle.action(PetAction.PLANT);
        return StepStatus.DONE;
    }
}
