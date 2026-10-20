package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.anim.state.PetAction;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import com.dwinovo.chiikawa.task.PetWorkCounters;
import com.dwinovo.chiikawa.utils.Utils;
import java.util.function.BiPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Breaks the block at a place, such as a weed or a mushroom, into the pet's bag: what it drops goes into the bag, and
 * anything that does not fit pops out where the block was. A plant pulled by hand does not wear the tool in the pet's
 * hand. Counts the block towards the slip the pet carries. Gives up if the block is not one to collect after all, or
 * is gone. Done in the tick it takes, so it is never cut half way: the block is collected and counted, or left as it
 * was. It does not look how near the pet is; the walk before it is what brings the pet within reach.
 */
public final class Collect implements Step {
    private final BlockPos target;
    private final BiPredicate<ServerLevel, BlockPos> qualifies;
    private final ResourceLocation workCounter;

    /**
     * @param target the block
     * @param qualifies whether the block there is still one to collect
     * @param workCounter the work each collected block reports, see {@link PetWorkCounters}
     */
    public Collect(BlockPos target, BiPredicate<ServerLevel, BlockPos> qualifies, ResourceLocation workCounter) {
        this.target = target;
        this.qualifies = qualifies;
        this.workCounter = workCounter;
    }

    /** @return a step that pulls up the weed at a place */
    public static Collect weed(BlockPos target) {
        return new Collect(target, Utils::isWeed, PetWorkCounters.WEED);
    }

    /** @return a step that picks the mushroom at a place */
    public static Collect mushroom(BlockPos target) {
        return new Collect(target, Utils::isMushroom, PetWorkCounters.PICK_MUSHROOM);
    }

    @Override
    public void begin(Handle handle) {
    }

    @Override
    public StepStatus tick(Handle handle) {
        AbstractPet pet = LiveHandle.of(handle).pet();
        if (!(pet.level() instanceof ServerLevel level) || !qualifies.test(level, target)) {
            return StepStatus.gaveUp("gone");
        }
        BlockState state = level.getBlockState(target);
        BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(target) : null;
        handle.action(PetAction.HARVEST);
        pet.dropResourcesToPetInv(state, level, target, blockEntity, pet.getMainHandItem());
        // Drops were already collected above, so don't drop again on removal.
        level.destroyBlock(target, false, pet);
        handle.reportWork(workCounter, 1);
        return StepStatus.DONE;
    }
}
