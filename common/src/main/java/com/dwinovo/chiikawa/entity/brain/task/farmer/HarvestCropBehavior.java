package com.dwinovo.chiikawa.entity.brain.task.farmer;


import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetMode;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.init.InitRegistry;

import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import java.util.Optional;
import com.dwinovo.chiikawa.utils.Utils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;
// Harvests a crop at the remembered position.
public class HarvestCropBehavior extends Behavior<AbstractPet>{
    // Required memories.
    private static final Map<MemoryModuleType<?>, MemoryStatus> REQUIRED_MEMORIES = ImmutableMap.of(
        InitMemory.HARVEST_POS.get(), MemoryStatus.VALUE_PRESENT
    );
    /**
     * Task with a short timeout.
     */
    public HarvestCropBehavior() {
        super(REQUIRED_MEMORIES, 100);
    }   
    /**
     * Checks whether harvesting can start.
     * @param world the server level
     * @param pet the pet entity
     * @return whether the task can start
     */
    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, AbstractPet pet) {
        if(
            pet.getPetMode() == PetMode.WORK && 
            pet.getPetJobId() == InitRegistry.FARMER_ID && 
            pet.getBrain().getMemory(InitMemory.HARVEST_POS.get()).isPresent() 
        )
        {
            Brain<AbstractPet> brain = pet.getBrain();
            BlockPos cropPos = brain.getMemory(InitMemory.HARVEST_POS.get()).get();
            return Utils.canHarvesr(world, cropPos) 
            && pet.getPetMode() == PetMode.WORK
            && pet.distanceToSqr(Vec3.atCenterOf(cropPos)) <= 1.0D;
        }
        return false;

    }
    /**
     * No extra setup needed.
     */
    @Override
    protected void start(ServerLevel world, AbstractPet pet, long time) {
    }
    @Override
    protected boolean canStillUse(ServerLevel world, AbstractPet pet, long time) {
        if (!super.canStillUse(world, pet, time)) {
            return false;
        }
        Optional<BlockPos> cropPosOpt = pet.getBrain().getMemory(InitMemory.HARVEST_POS.get());
        if (cropPosOpt.isEmpty()) {
            return false;
        }
        BlockPos cropPos = cropPosOpt.get();
        return Utils.canHarvesr(world, cropPos)
            && pet.distanceToSqr(Vec3.atCenterOf(cropPos)) <= 1.0D
            && pet.getPetMode() == PetMode.WORK
            && pet.getPetJobId() == InitRegistry.FARMER_ID;
    }
    
    /**
     * Performs the actual harvest on stop.
     * @param world the server level
     * @param pet the pet entity
     * @param pGameTime the current time
     */
    @Override
    protected void stop(ServerLevel world, AbstractPet pet, long pGameTime) {
        Brain<AbstractPet> brain = pet.getBrain();
        Optional<BlockPos> cropPosOpt = brain.getMemory(InitMemory.HARVEST_POS.get());
        if(cropPosOpt.isEmpty()) return;
        BlockPos cropPos = cropPosOpt.get();

        if (!world.isClientSide()) {
            ItemStack tool = pet.getMainHandItem();
            if (Utils.canHarvesr(world, cropPos)) {
                pet.triggerAnim("use_mainhand");
                world.destroyBlock(cropPos, true, pet);
                tool.hurtAndBreak(1, pet, (e) -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
            }
        }
        pet.getBrain().eraseMemory(InitMemory.HARVEST_POS.get());
    }
}
    
    



