package com.dwinovo.chiikawa.entity.brain.task.farmer;


import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetMode;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.init.InitRegistry;
import com.dwinovo.chiikawa.utils.Utils;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Optional;

import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import net.minecraft.server.level.ServerLevel;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
// Plants a crop at the remembered position.
public class PlantCropBehavior extends Behavior<AbstractPet>{
   
    // Required memories.
    private static final Map<MemoryModuleType<?>, MemoryStatus> REQUIRED_MEMORIES = ImmutableMap.of(
        InitMemory.PLANT_POS.get(), MemoryStatus.VALUE_PRESENT
    );
    /**
     * Task with a short timeout.
     */
    public PlantCropBehavior() {
        super(REQUIRED_MEMORIES, 100);
    }
    /**
     * Checks whether planting can start.
     * @param world the server level
     * @param pet the pet entity
     * @return whether the task can start
     */ 
    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, AbstractPet pet) {
        if(
            pet.getPetMode() == PetMode.WORK && 
            pet.getPetJobId() == InitRegistry.FARMER_ID && 
            !pet.getBrain().getMemory(InitMemory.HARVEST_POS.get()).isPresent() && 
            pet.getBrain().getMemory(InitMemory.PLANT_POS.get()).isPresent() 
        )
        {
            BlockPos farmlandPos = pet.getBrain().getMemory(InitMemory.PLANT_POS.get()).get();
            if( 
                pet.distanceToSqr(Vec3.atCenterOf(farmlandPos)) <= 2.0D
                && Utils.isCanPlantFarmland(world, farmlandPos)
            ){
                return true;
            }
        }
        return false;
    }
    @Override
    protected boolean canStillUse(ServerLevel world, AbstractPet pet, long time) {
        if (!super.canStillUse(world, pet, time)) {
            return false;
        }
        if (pet.getPetMode() != PetMode.WORK || pet.getPetJobId() != InitRegistry.FARMER_ID) {
            return false;
        }
        if (pet.getBrain().getMemory(InitMemory.HARVEST_POS.get()).isPresent()) {
            return false;
        }
        Optional<BlockPos> farmlandPosOpt = pet.getBrain().getMemory(InitMemory.PLANT_POS.get());
        if (farmlandPosOpt.isEmpty()) {
            return false;
        }
        BlockPos farmlandPos = farmlandPosOpt.get();
        return Utils.isCanPlantFarmland(world, farmlandPos)
            && pet.distanceToSqr(Vec3.atCenterOf(farmlandPos)) <= 2.0D
            && !Utils.getSeed(pet).isEmpty();
    }
    /**
     * No extra setup needed.
     */
    @Override
    protected void start(ServerLevel world, AbstractPet pet, long time) {
    }
    /**
     * Plants a seed on stop.
     * @param world the server level
     * @param pet the pet entity
     * @param pGameTime the current time
     */
    @Override
    protected void stop(ServerLevel world, AbstractPet pet, long pGameTime) {
        Brain<AbstractPet> brain = pet.getBrain();
        Optional<BlockPos> farmlandPosOpt = brain.getMemory(InitMemory.PLANT_POS.get());
        if(farmlandPosOpt.isEmpty()) return;
        BlockPos farmlandPos = farmlandPosOpt.get();
        ItemStack seed = Utils.getSeed(pet);
        Optional<BlockState> cropState = getPlantBlockState(seed);
        if (cropState.isPresent() && Utils.isCanPlantFarmland(world, farmlandPos)) {
            pet.triggerAnim("use_mainhand");
            world.setBlock(farmlandPos.above(), cropState.get(), 2);
            seed.shrink(1);
        }
        pet.getBrain().eraseMemory(InitMemory.PLANT_POS.get());
    }

    private static Optional<BlockState> getPlantBlockState(ItemStack seed) {
        if (seed.isEmpty()) {
            return Optional.empty();
        }
        Item item = seed.getItem();
        if (item instanceof BlockItem blockItem) {
            return Optional.of(blockItem.getBlock().defaultBlockState());
        }
        return Optional.empty();
    }
}

