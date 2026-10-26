package com.dwinovo.chiikawa.entity.brain.errand.run;

import com.dwinovo.chiikawa.anim.state.PetAction;
import com.dwinovo.chiikawa.anim.state.PetReaction;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.Cue;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.Message;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import java.util.Optional;
import java.util.OptionalInt;
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

    /** Sets off for a point in the open, wherever it falls in its block. */
    void walkTo(Vec3 target, float speed, int closeEnough);

    /** Sets off for a creature, following it as it moves. */
    void walkTo(Entity target, float speed, int closeEnough);

    /**
     * Stops walking: forgets where it was going, the path to it, and that it could not find one, and has the brain's mover
     * let go of the walk it was on, so that a walk set next is planned afresh.
     */
    void stopWalking();

    /**
     * @return for how many ticks the brain's own mover has said it has no way to where the pet walks to, which is a
     *         path it cannot find or one that stops short; empty while it has one
     */
    OptionalInt blockedTicks();

    /**
     * @return whether the pet is still on the walk it was sent on: the brain's mover lets go of it when the pet has
     *         arrived, when it finds no way, and when it has gone as far along a path as the path goes
     */
    boolean walking();

    /**
     * @return whether the brain's mover can look for a way just now, which it cannot from the air: a pet in a hop or a
     *         fall finds no path to anywhere, and says so
     */
    boolean canFindWay();

    void lookAt(Entity target);

    /** Looks at a creature for this long, and then lets go of the gaze by itself. */
    void lookAt(Entity target, int ticks);

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

    default void say(VoiceMoment moment) {
        say(moment, Optional.empty());
    }

    /**
     * Says something fitting the moment about something, if the pet has something to say and has not said too much of late.
     *
     * @param subject the translation key of what the line is about, for a line that has the place for it
     * @return whether it said it
     */
    boolean say(VoiceMoment moment, Optional<String> subject);

    void tellOwner(Component message);

    void startUsingMainHand();

    void stopUsingMainHand();

    void swing();

    /** Holds off the pet's next attack for this long. */
    void cooldownAttack(int ticks);

    /**
     * Tells how the errand is faring, as its settings name the report: the pet rests the errand as they say
     * for it, instead of what it was resting, whether that is more or less. For a step that does something
     * the errand must keep its distance from afterwards even if it is cut short, such as pressing a shutter.
     * A report the settings do not name changes nothing.
     */
    void report(String report);

    /** Counts work done towards the slip the pet carries. */
    void reportWork(ResourceLocation counter, int amount);

    /** Tells another pet something, as the inbox of that pet takes it. */
    void post(AbstractPet other, Message message);

    /** Has another pet do something at once and in passing, as that pet takes a cue. */
    void cue(AbstractPet other, Cue cue);

    /** @return the current game time */
    long now();
}
