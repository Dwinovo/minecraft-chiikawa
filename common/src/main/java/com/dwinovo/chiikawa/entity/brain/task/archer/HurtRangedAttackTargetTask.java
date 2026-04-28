package com.dwinovo.chiikawa.entity.brain.task.archer;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetMode;
import com.dwinovo.chiikawa.init.InitRegistry;
import com.dwinovo.chiikawa.utils.Utils;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class HurtRangedAttackTargetTask extends Behavior<AbstractPet>{
    private int actionTime = 20;
    private static final Map<MemoryModuleType<?>, MemoryStatus> REQUIRED_MEMORIES = ImmutableMap.of(
        MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT
    );
    public HurtRangedAttackTargetTask(){
        super(REQUIRED_MEMORIES, 15);
    }
    @Override
    protected boolean checkExtraStartConditions(ServerLevel pLevel, AbstractPet pet) {
        // Require work mode, archer job, valid target, and no cooldown.
        return pet.getPetMode() == PetMode.WORK 
        && pet.getPetJobId() == InitRegistry.ARCHER_ID
        && pet.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)
        && pet.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).get().isAlive()
        && pet.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).get().distanceTo(pet) <= 15.0F
        && !pet.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_COOLING_DOWN)
        && !Utils.getArrow(pet).isEmpty();
    }
    @Override
    protected void start(ServerLevel pLevel, AbstractPet pet, long pGameTime) {
        if (pet.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).isPresent())
        {
            LivingEntity target = pet.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).get();
            pet.triggerAnim("sword_attack");
            pet.startUsingItem(InteractionHand.MAIN_HAND);
            pet.getLookControl().setLookAt(target, 30.0f, 30.0f);
        }

    }
    @Override
    protected void tick(ServerLevel pLevel, AbstractPet pet, long pGameTime) {
        if(pet.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).isPresent())
        {
            LivingEntity target = pet.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).get();
            pet.getLookControl().setLookAt(target, 30.0f, 30.0f);
            pet.getNavigation().stop();
        }
        actionTime--;
    }
    @Override
    protected boolean canStillUse(ServerLevel pLevel, AbstractPet pEntity, long pGameTime) {
        if (actionTime <= 0 || Utils.getArrow(pEntity).isEmpty()) {
            return false;
        }
        if (!pEntity.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) {
            return false;
        }
        LivingEntity target = pEntity.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).get();
        return target.isAlive() && target.distanceTo(pEntity) <= 15.0F;
    }
    @Override
    protected void stop(ServerLevel pLevel, AbstractPet pEntity, long pGameTime) {
        if (pEntity.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).isPresent()) {
            pEntity.performRangedAttack(pEntity.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).get(), 1.0f);
            
        }
        pEntity.getBrain().setMemoryWithExpiry(MemoryModuleType.ATTACK_COOLING_DOWN, true, 20);
        pEntity.stopUsingItem();
        actionTime = 20;
    }


}

