package com.dwinovo.chiikawa.entity.brain.errand.inbox;

import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.photo.PhotoSubject;
import com.dwinovo.chiikawa.task.PetTask;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * What is told to a pet from outside its own mind: the owner changed something, another pet asked
 * something, a photo came back. Each is a plain record of ids, numbers and positions, never of an
 * entity, so a message waiting in an inbox cannot keep a pet that has since gone from the world
 * alive; whoever handles it finds the pet again by its id, and finds it gone.
 */
public sealed interface Message {
    /**
     * Something may have changed that the pet should decide again about, such as being fed or paid.
     *
     * @param cause what, as a short key for the log
     */
    record Nudge(String cause) implements Message {
    }

    /** The owner told the pet what to do in general: follow, stay, or be free. */
    record DirectiveChanged() implements Message {
    }

    /** The pet was given a tool or lost one, and is able to do other work. */
    record CapabilityChanged() implements Message {
    }

    /**
     * The pet was hurt.
     *
     * @param attacker who did it, if somebody did
     */
    record Hurt(Optional<UUID> attacker) implements Message {
    }

    /**
     * The pet was taken somewhere without walking there.
     *
     * @param cause what took it, as a short key for the log
     */
    record Moved(String cause) implements Message {
    }

    /**
     * The owner's whistle, to a pet of theirs.
     *
     * @param order what the pet is told to do
     * @param maxDelay the longest the pet waits before it answers, in ticks; it picks its own
     */
    record WhistleOrder(PetDirective order, int maxDelay) implements Message {
    }

    /**
     * A whistle a wild pet heard.
     *
     * @param where the blower, as they stood when they blew
     * @param from the game time the pet sets off for it
     * @param until the game time it loses interest
     */
    record WhistleHeard(GlobalPos where, long from, long until) implements Message {
    }

    /**
     * The exam the pet is signed up for will not be held.
     *
     * @param id the qualification
     * @param reason why, as a short key for the owner's message
     */
    record ExamCalledOff(ResourceLocation id, String reason) implements Message {
    }

    /**
     * The results of an exam are out and the pet has not heard them.
     *
     * @param id the qualification
     */
    record ResultsDue(ResourceLocation id) implements Message {
    }

    /**
     * The pet finished the work on its slip.
     *
     * @param task the slip, as it was when it was finished
     */
    record SlipCompleted(PetTask task) implements Message {
    }

    /** What came back of a photo the pet asked its owner's game for. */
    sealed interface PhotoResult extends Message {
        /**
         * The picture was kept.
         *
         * @param photo its id
         * @param kind what it is of
         * @param name who is in it, if somebody is
         * @param biome where it was taken
         * @param camera the camera item it was taken with
         */
        record Stored(UUID photo, PhotoSubject kind, String name, Optional<ResourceLocation> biome,
                      ResourceLocation camera) implements PhotoResult {
        }

        /**
         * The picture is not coming.
         *
         * @param photo its id
         * @param camera the camera item it was taken with
         */
        record Failed(UUID photo, ResourceLocation camera) implements PhotoResult {
        }
    }

    /**
     * The song the pet was playing has stopped.
     *
     * @param reason why, as a short key for the log
     */
    record MusicStopped(String reason) implements Message {
    }

    /**
     * Another pet asks this one to play a scene with it.
     *
     * @param initiator who asks
     * @param scene which scene
     * @param partnerRank which of the scene's partners the asked pet would be
     * @param askPriority how much the asking pet's errand matters: the asked one is only called away
     *                    from something of a lower priority
     * @param expiresAt the game time after which the asking pet no longer waits
     */
    record Invitation(UUID initiator, ResourceLocation scene, int partnerRank, int askPriority, long expiresAt)
            implements Message {
    }

    /**
     * The answer to an invitation.
     *
     * @param from who answers
     * @param accepted whether it will come
     */
    record InvitationReply(UUID from, boolean accepted) implements Message {
    }

    /**
     * The asking pet has arrived and the scene begins.
     *
     * @param treat what it hands over, a copy that is the receiver's to keep
     * @param eagerTicks for how long the receiver is keen after eating it
     */
    record SceneStart(ItemStack treat, int eagerTicks) implements Message {
    }

    /** The scene was played to the end. */
    record SceneOver() implements Message {
    }

    /**
     * The pet that asked, or that was asked, will not play after all.
     *
     * @param from who
     */
    record Withdrawn(UUID from) implements Message {
    }
}
