package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.anim.state.PetAction;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import com.dwinovo.chiikawa.utils.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * Harvests the ripe crop at a place, into the pet's bag, and the crop is put back as the crop's own handler has it, a
 * young plant where the ripe one stood. The tool in the pet's hand wears a little. Gives up if the crop is not ripe
 * after all, or is gone. Done in the tick it takes, so it is never cut half way: the crop is reaped and replanted, or
 * left as it was. It does not look how near the pet is; the walk before it is what brings the pet within reach.
 */
public final class Harvest implements Step {
    private final BlockPos crop;

    /** @param crop the crop's block */
    public Harvest(BlockPos crop) {
        this.crop = crop;
    }

    @Override
    public void begin(Handle handle) {
    }

    @Override
    public StepStatus tick(Handle handle) {
        AbstractPet pet = LiveHandle.of(handle).pet();
        if (!(pet.level() instanceof ServerLevel level) || !Utils.canHarvesr(level, crop)) {
            return StepStatus.gaveUp("gone");
        }
        handle.action(PetAction.HARVEST);
        Utils.harvestCrop(level, pet, crop);
        pet.getMainHandItem().hurtAndBreak(1, pet, EquipmentSlot.MAINHAND);
        return StepStatus.DONE;
    }
}
