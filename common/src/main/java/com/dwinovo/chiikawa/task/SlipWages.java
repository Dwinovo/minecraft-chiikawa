package com.dwinovo.chiikawa.task;

import com.dwinovo.chiikawa.anim.state.PetReaction;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.voice.PetSpeech;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * What a pet is paid for a finished slip, and how it takes it. The wages go straight into its backpack wherever
 * it is, and what does not fit lands at its feet; the pet's mind pays them as it is told the slip is finished, see
 * {@code Message.SlipCompleted}.
 */
public final class SlipWages {
    private static final int REWARD_PARTICLES = 12;

    private SlipWages() {
    }

    /**
     * Pays the slip out: the reward into the pet's backpack, the slip noted as the last it finished, and one more
     * slip of practice for the licences it is practice for. Server-side.
     *
     * @param task the slip, as it was when it was finished
     */
    public static void pay(AbstractPet pet, PetTask task) {
        ServerLevel level = (ServerLevel) pet.level();
        pet.getBrain().setMemory(InitMemory.LAST_FINISHED_SLIP.get(), new FinishedSlip(task.type(), level.getGameTime()));
        pet.licences().practised(task.type());
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
    }

    /** The pet shows it has been paid: a sparkle, joy, the happy sound and something to say. Server-side. */
    public static void rejoice(AbstractPet pet) {
        ServerLevel level = (ServerLevel) pet.level();
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pet.getX(), pet.getY() + pet.getBbHeight() * 0.8, pet.getZ(),
            REWARD_PARTICLES, 0.35, 0.3, 0.35, 0.0);
        pet.triggerReaction(PetReaction.HAPPY);
        pet.playTameSound();
        PetSpeech.say(pet, VoiceMoment.PAID);
    }
}
