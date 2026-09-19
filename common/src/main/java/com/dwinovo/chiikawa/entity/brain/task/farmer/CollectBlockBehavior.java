package com.dwinovo.chiikawa.entity.brain.task.farmer;

import com.dwinovo.chiikawa.anim.state.PetAction;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.task.PetWorkCounters;
import com.dwinovo.chiikawa.task.TaskTracker;
import com.dwinovo.chiikawa.utils.Utils;
import com.google.common.collect.ImmutableMap;
import java.util.function.BiPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

// Breaks the remembered block, such as a weed or a mushroom, into the pet's backpack.
public class CollectBlockBehavior extends Behavior<AbstractPet> {
    private final MemoryModuleType<BlockPos> target;
    private final BiPredicate<ServerLevel, BlockPos> qualifies;
    private final ResourceLocation workCounter;

    /**
     * Creates the task.
     * @param target the memory holding the block to collect
     * @param qualifies whether the block there is still one to collect
     * @param workCounter the work each collected block reports, see {@link PetWorkCounters}
     */
    public CollectBlockBehavior(MemoryModuleType<BlockPos> target, BiPredicate<ServerLevel, BlockPos> qualifies,
            ResourceLocation workCounter) {
        super(ImmutableMap.of(target, MemoryStatus.VALUE_PRESENT));
        this.target = target;
        this.qualifies = qualifies;
        this.workCounter = workCounter;
    }

    /**
     * Checks whether the pet stands next to a block it can collect.
     * @param world the server level
     * @param pet the pet entity
     * @return whether the task can start
     */
    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, AbstractPet pet) {
        BlockPos pos = pet.getBrain().getMemory(target).orElseThrow();
        return qualifies.test(world, pos) && pet.distanceToSqr(Vec3.atCenterOf(pos)) <= Utils.WORK_REACH_SQR;
    }

    /**
     * Collects the block at once: its drops go into the backpack (anything that does not
     * fit pops out where the block was) and the block is removed. A plant pulled by hand
     * does not wear the held tool.
     * @param world the server level
     * @param pet the pet entity
     * @param time the current time
     */
    @Override
    protected void start(ServerLevel world, AbstractPet pet, long time) {
        BlockPos pos = pet.getBrain().getMemory(target).orElseThrow();
        BlockState state = world.getBlockState(pos);
        BlockEntity blockEntity = state.hasBlockEntity() ? world.getBlockEntity(pos) : null;
        pet.triggerAction(PetAction.HARVEST);
        pet.dropResourcesToPetInv(state, world, pos, blockEntity, pet.getMainHandItem());
        // Drops were already collected above, so don't drop again on removal.
        world.destroyBlock(pos, false, pet);
        pet.getBrain().eraseMemory(target);
        TaskTracker.advance(pet, workCounter, 1);
    }
}
