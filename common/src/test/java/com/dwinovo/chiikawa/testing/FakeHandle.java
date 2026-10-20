package com.dwinovo.chiikawa.testing;

import com.dwinovo.chiikawa.anim.state.PetAction;
import com.dwinovo.chiikawa.anim.state.PetReaction;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.Message;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** A handle on no pet, that counts how often the walk and the gaze were cleared and notes what it was asked to say. */
public final class FakeHandle implements Handle {
    public int walkCleared;
    public int lookCleared;
    public long now;
    public final List<VoiceMoment> said = new ArrayList<>();

    @Override
    public void stopWalking() {
        walkCleared++;
    }

    @Override
    public void clearLook() {
        lookCleared++;
    }

    @Override
    public long now() {
        return now;
    }

    @Override
    public void walkTo(BlockPos target, float speed, int closeEnough) {
    }

    @Override
    public void walkTo(Entity target, float speed, int closeEnough) {
    }

    @Override
    public int blockedTicks() {
        return 0;
    }

    @Override
    public void lookAt(Entity target) {
    }

    @Override
    public void lookAt(Vec3 target) {
    }

    @Override
    public void lookAt(BlockPos target) {
    }

    @Override
    public void snapTo(Vec3 position, float yaw) {
    }

    @Override
    public void faceYaw(float yaw) {
    }

    @Override
    public void action(PetAction action) {
    }

    @Override
    public void reaction(PetReaction reaction) {
    }

    @Override
    public void gesture(String name) {
    }

    @Override
    public void say(VoiceMoment moment) {
        said.add(moment);
    }

    @Override
    public void tellOwner(Component message) {
    }

    @Override
    public void startUsingMainHand() {
    }

    @Override
    public void stopUsingMainHand() {
    }

    @Override
    public void swing() {
    }

    @Override
    public void cooldownAttack(int ticks) {
    }

    @Override
    public void reportWork(ResourceLocation counter, int amount) {
    }

    @Override
    public void post(AbstractPet other, Message message) {
    }
}
