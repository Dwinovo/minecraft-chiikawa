package com.dwinovo.chiikawa.testing;

import com.dwinovo.chiikawa.anim.state.PetAction;
import com.dwinovo.chiikawa.anim.state.PetReaction;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.Cue;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.Message;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** A handle on no pet, that counts how often the walk and the gaze were cleared or glanced away and notes what it was asked to say. */
public final class FakeHandle implements Handle {
    public int walkCleared;
    public int lookCleared;
    public int glances;
    public long now;
    public final List<VoiceMoment> said = new ArrayList<>();
    public final List<Optional<String>> saidAbout = new ArrayList<>();
    public final List<String> gestures = new ArrayList<>();
    public final List<String> reported = new ArrayList<>();

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
    public void walkTo(Vec3 target, float speed, int closeEnough) {
    }

    @Override
    public void walkTo(Entity target, float speed, int closeEnough) {
    }

    @Override
    public OptionalInt blockedTicks() {
        return OptionalInt.empty();
    }

    @Override
    public boolean walking() {
        return true;
    }

    @Override
    public boolean canFindWay() {
        return true;
    }

    @Override
    public void lookAt(Entity target) {
    }

    @Override
    public void lookAt(Entity target, int ticks) {
        glances++;
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
        gestures.add(name);
    }

    @Override
    public boolean say(VoiceMoment moment, Optional<String> subject) {
        said.add(moment);
        saidAbout.add(subject);
        return true;
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
    public void report(String report) {
        reported.add(report);
    }

    @Override
    public void reportWork(ResourceLocation counter, int amount) {
    }

    @Override
    public void post(AbstractPet other, Message message) {
    }

    @Override
    public void cue(AbstractPet other, Cue cue) {
    }
}
