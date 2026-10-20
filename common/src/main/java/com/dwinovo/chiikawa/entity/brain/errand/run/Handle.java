package com.dwinovo.chiikawa.entity.brain.errand.run;

import com.dwinovo.chiikawa.anim.state.PetAction;
import com.dwinovo.chiikawa.anim.state.PetReaction;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.Message;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * What a step may do to its pet: the whole of it. The executor is the only one that moves a pet's
 * walk, gaze and path, and it does so through this, so what a step has done is what the handle was
 * asked, whatever stands behind it.
 */
public interface Handle {
    /** Sets off for a place, to within {@code closeEnough} blocks of it. */
    void walkTo(BlockPos target, float speed, int closeEnough);

    /** Sets off for a creature, following it as it moves. */
    void walkTo(Entity target, float speed, int closeEnough);

    /** Stops walking: forgets where it was going and the path to it. */
    void stopWalking();

    /** @return for how many ticks the pet has wanted to walk and been unable to */
    int blockedTicks();

    void lookAt(Entity target);

    void lookAt(Vec3 target);

    void lookAt(BlockPos target);

    /** Looks where it is going again. */
    void clearLook();

    /** Puts the pet down at a place, turned to a heading, without walking. */
    void snapTo(Vec3 position, float yaw);

    void faceYaw(float yaw);

    void action(PetAction action);

    void reaction(PetReaction reaction);

    /** Strikes a pose of the pet's own by name, if it has one. */
    void gesture(String name);

    void say(VoiceMoment moment);

    void tellOwner(Component message);

    void startUsingMainHand();

    void stopUsingMainHand();

    void swing();

    /** Holds off the pet's next attack for this long. */
    void cooldownAttack(int ticks);

    /** Counts work done towards the slip the pet carries. */
    void reportWork(ResourceLocation counter, int amount);

    /** Tells another pet something, as the inbox of that pet takes it. */
    void post(AbstractPet other, Message message);

    /** @return the current game time */
    long now();
}
