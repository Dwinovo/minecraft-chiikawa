package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.anim.state.PetAction;
import com.dwinovo.chiikawa.anim.state.PetReaction;
import com.dwinovo.chiikawa.block.ExamDeskBlockEntity;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetGesture;
import com.dwinovo.chiikawa.entity.brain.PetMind;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.Cue;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.Message;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.task.TaskTracker;
import com.dwinovo.chiikawa.voice.PetSpeech;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * A live pet behind the {@link Handle}: what a step asks is carried out on the pet's brain and body.
 * The vanilla sinks of the brain's core then walk and look in the same tick.
 *
 * <p>The pet itself is reachable from this package only, so that the steps of the library, which
 * reach into the world for a crop or a chest, are the one place that can; the errands that compose
 * them reach the pet through the handle alone.
 */
public final class LiveHandle implements Handle {
    private final AbstractPet pet;

    public LiveHandle(AbstractPet pet) {
        this.pet = pet;
    }

    AbstractPet pet() {
        return pet;
    }

    /** @return the handle a step was given, which in the library's steps is always a live one */
    static LiveHandle of(Handle handle) {
        return (LiveHandle) handle;
    }

    /** @return the creature or item with this id, while it is in the pet's world and has not gone */
    Optional<Entity> creature(UUID id) {
        return pet.level() instanceof ServerLevel level ? Optional.ofNullable(level.getEntity(id)).filter(Entity::isAlive)
            : Optional.empty();
    }

    /** @return the exam desk at that place, while it is in the pet's world and loaded and has not been broken */
    Optional<ExamDeskBlockEntity> examDesk(GlobalPos desk) {
        return ExamDeskBlockEntity.at(pet.level(), desk);
    }

    /** @return the pet's owner, while they are a player in the pet's world */
    Optional<Player> owner() {
        return pet.getOwner() instanceof Player owner && owner.isAlive() ? Optional.of(owner) : Optional.empty();
    }

    private Brain<AbstractPet> brain() {
        return pet.getBrain();
    }

    @Override
    public void walkTo(BlockPos target, float speed, int closeEnough) {
        brain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(target, speed, closeEnough));
    }

    @Override
    public void walkTo(Entity target, float speed, int closeEnough) {
        brain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new EntityTracker(target, false), speed, closeEnough));
    }

    @Override
    public void stopWalking() {
        brain().eraseMemory(MemoryModuleType.WALK_TARGET);
        brain().eraseMemory(MemoryModuleType.PATH);
        brain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
    }

    @Override
    public int blockedTicks() {
        return (int) brain().getMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE)
            .map(since -> now() - since).orElse(0L).longValue();
    }

    @Override
    public void lookAt(Entity target) {
        brain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));
    }

    @Override
    public void lookAt(Entity target, int ticks) {
        brain().setMemoryWithExpiry(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true), ticks);
    }

    @Override
    public void lookAt(Vec3 target) {
        brain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(target));
    }

    @Override
    public void lookAt(BlockPos target) {
        brain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(target));
    }

    @Override
    public void clearLook() {
        brain().eraseMemory(MemoryModuleType.LOOK_TARGET);
    }

    @Override
    public void snapTo(Vec3 position, float yaw) {
        pet.moveTo(position.x, position.y, position.z, yaw, 0.0F);
        faceYaw(yaw);
    }

    @Override
    public void faceYaw(float yaw) {
        pet.setYRot(yaw);
        pet.setYBodyRot(yaw);
        pet.setYHeadRot(yaw);
    }

    @Override
    public void action(PetAction action) {
        pet.triggerAction(action);
    }

    @Override
    public void reaction(PetReaction reaction) {
        pet.triggerReaction(reaction);
    }

    @Override
    public void gesture(String name) {
        PetGesture.make(pet, name);
    }

    @Override
    public void say(VoiceMoment moment) {
        PetSpeech.say(pet, moment);
    }

    @Override
    public void tellOwner(Component message) {
        pet.tellOwner(message);
    }

    @Override
    public void startUsingMainHand() {
        pet.startUsingItem(InteractionHand.MAIN_HAND);
    }

    @Override
    public void stopUsingMainHand() {
        pet.stopUsingItem();
    }

    @Override
    public void swing() {
        pet.swing(InteractionHand.MAIN_HAND);
    }

    @Override
    public void cooldownAttack(int ticks) {
        brain().setMemoryWithExpiry(MemoryModuleType.ATTACK_COOLING_DOWN, true, ticks);
    }

    @Override
    public void report(String report) {
        pet.getMind().report(report);
    }

    @Override
    public void reportWork(ResourceLocation counter, int amount) {
        TaskTracker.advance(pet, counter, amount);
    }

    @Override
    public void post(AbstractPet other, Message message) {
        PetMind.post(other, message);
    }

    @Override
    public void cue(AbstractPet other, Cue cue) {
        PetMind.cue(other, cue);
    }

    @Override
    public long now() {
        return pet.level().getGameTime();
    }
}
