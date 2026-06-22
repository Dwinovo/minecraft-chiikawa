package com.dwinovo.chiikawa.entity.brain.task.farmer;


import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetMode;

import net.minecraft.server.level.ServerLevel;

import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.init.InitRegistry;
import com.dwinovo.chiikawa.utils.Utils;

import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import com.google.common.collect.ImmutableMap;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import java.util.Map;
// Walks to a harvestable crop.
public class WalkToHarvestCropBehavior extends Behavior<AbstractPet>{
    // Required memories.
    private static final Map<MemoryModuleType<?>, MemoryStatus> REQUIRED_MEMORIES = ImmutableMap.of(
        InitMemory.HARVEST_POS.get(), MemoryStatus.VALUE_PRESENT
    );
    // Move speed.
    private final float speed;
    /**
     * Creates the task.
     * @param speed move speed
     */
    public WalkToHarvestCropBehavior(float speed) {
        super(REQUIRED_MEMORIES, 15);
        this.speed = speed;
    }
    /**
     * Checks whether the task can start.
     * @param world the server level
     * @param pet the pet entity
     * @return whether the task can start
     */
    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, AbstractPet pet) {
        if (pet.getPetMode() == PetMode.WORK && pet.getPetJobId() == InitRegistry.FARMER_ID 
            && pet.getBrain().getMemory(InitMemory.HARVEST_POS.get()).isPresent()
        ){
            net.minecraft.core.BlockPos target = pet.getBrain().getMemory(InitMemory.HARVEST_POS.get()).get();
            if (Utils.canHarvesr(world, target)) {
                // Single reachability pathfind for the chosen target (the sensor no
                // longer does this per-candidate). If unreachable, blacklist so the
                // next scan moves on instead of re-picking it.
                if (Utils.canReach(pet, target)) {
                    return true;
                }
                pet.blacklistUnreachable(target);
            }
            pet.getBrain().eraseMemory(InitMemory.HARVEST_POS.get());
        }
        return false;
    }
    /**
     * Start walking to the target.
     * @param world the server level
     * @param pet the pet entity
     * @param time the current time
     */
    @Override
    protected void start(ServerLevel world, AbstractPet pet, long time) {
        BehaviorUtils.setWalkAndLookTargetMemories(pet, pet.getBrain().getMemory(InitMemory.HARVEST_POS.get()).get(), speed, 0);
    }
}

