package com.dwinovo.chiikawa.task;

import com.dwinovo.chiikawa.anim.state.PetReaction;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.intent.IntentSelector;
import com.dwinovo.chiikawa.init.InitMemory;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * Counts the work a pet reports against the slip it carries, and pays the slip out the
 * moment it is finished: wherever the pet is, the reward goes straight into its
 * backpack with a sparkle.
 */
public final class TaskTracker {
    private static final int REWARD_PARTICLES = 12;

    private TaskTracker() {
    }

    /**
     * Reports work a pet just did. Server-side.
     *
     * @param counter the kind of work, see {@link PetWorkCounters}
     * @param amount how much of it
     */
    public static void advance(AbstractPet pet, ResourceLocation counter, int amount) {
        pet.getTask().filter(task -> task.counter().equals(counter)).ifPresent(task -> {
            PetTask advanced = task.advance(amount);
            if (advanced.isDone()) {
                complete(pet, advanced);
            } else {
                pet.setTask(advanced);
            }
        });
    }

    private static void complete(AbstractPet pet, PetTask task) {
        ServerLevel level = (ServerLevel) pet.level();
        pet.setTask(null);
        pet.getBrain().setMemory(InitMemory.LAST_FINISHED_SLIP.get(), new FinishedSlip(task.type(), level.getGameTime()));
        LootTable reward = level.getServer().reloadableRegistries().getLootTable(task.reward());
        LootParams params = new LootParams.Builder(level)
            .withParameter(LootContextParams.ORIGIN, pet.position())
            .withParameter(LootContextParams.THIS_ENTITY, pet)
            .create(LootContextParamSets.GIFT);
        for (ItemStack stack : reward.getRandomItems(params)) {
            ItemStack remainder = pet.getBackpack().addItem(stack);
            if (!remainder.isEmpty()) {
                pet.spawnAtLocation(remainder);
            }
        }
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pet.getX(), pet.getY() + pet.getBbHeight() * 0.8, pet.getZ(),
            REWARD_PARTICLES, 0.35, 0.3, 0.35, 0.0);
        pet.triggerReaction(PetReaction.HAPPY);
        pet.playTameSound();
        IntentSelector.requestReevaluate(pet);
    }
}
