package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.anim.state.PetAction;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import com.dwinovo.chiikawa.utils.Utils;
import java.util.Optional;
import java.util.function.BiPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * Breaks the block at a place into the pet's bag, as the block's own crop handler has it: what it drops goes into the
 * bag, anything that does not fit pops out where the block was, and a ripe crop is put back as a young plant where it
 * stood while any other block is broken, such as a weed or a mushroom. The tool in the pet's hand wears as much as the
 * work says, which is nothing for a plant pulled by hand. Counts the block towards the slip the pet carries, if the
 * work has one. Gives up if the block is not one to harvest after all, or is gone. Done in the tick it takes, so it is
 * never cut half way: the block is harvested and counted, or left as it was. It does not look how near the pet is; the
 * walk before it is what brings the pet within reach.
 */
public final class Harvest implements Step {
    private final BlockPos target;
    private final BiPredicate<ServerLevel, BlockPos> holds;
    private final Optional<ResourceLocation> workCounter;
    private final int toolWear;

    /**
     * @param target the block
     * @param holds whether the block there is still one to harvest
     * @param workCounter the work each harvested block reports, if the work has one
     * @param toolWear how much the tool in the pet's hand wears each time, none for none
     */
    public Harvest(BlockPos target, BiPredicate<ServerLevel, BlockPos> holds, Optional<ResourceLocation> workCounter,
            int toolWear) {
        this.target = target;
        this.holds = holds;
        this.workCounter = workCounter;
        this.toolWear = toolWear;
    }

    @Override
    public void begin(Handle handle) {
    }

    @Override
    public StepStatus tick(Handle handle) {
        AbstractPet pet = LiveHandle.of(handle).pet();
        if (!(pet.level() instanceof ServerLevel level) || !holds.test(level, target)) {
            return StepStatus.gaveUp("gone");
        }
        handle.action(PetAction.HARVEST);
        Utils.harvestCrop(level, pet, target);
        if (toolWear > 0) {
            pet.getMainHandItem().hurtAndBreak(toolWear, pet, EquipmentSlot.MAINHAND);
        }
        workCounter.ifPresent(counter -> handle.reportWork(counter, 1));
        return StepStatus.DONE;
    }
}
