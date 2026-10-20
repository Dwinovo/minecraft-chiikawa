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
 *
 * <p>A message is read at the pet's next tick, and one that waits when the pet is removed from the world
 * is lost with the inbox. So what a message makes durable, anything saved with the pet or the world such as a
 * shot on its film roll, is not left to the tick: a {@link Durable} message is applied as it is told, and
 * only what concerns deciding and the errand under way, a rest, a step's ending, a nudge, waits in the inbox
 * as a {@link Settled} one. See {@code PetMind}.
 */
public sealed interface Message {
    /**
     * A message with something to keep: it has effects that must outlive the inbox, which the pet's mind
     * applies the moment it is told, on the server thread, and then queues what is left of it as {@link Settled}.
     * Each kind is applied in one place, the mind's {@code keep}, which is the only writer of what it makes
     * durable.
     */
    sealed interface Durable extends Message permits PhotoResult {
    }

    /**
     * A {@link Durable} message after its durable effects were applied, which is the form the inbox holds:
     * what is left for the next tick is to decide and to tell the errand under way.
     *
     * @param message what was told
     * @param report what the durable part found, for the rest ledger to rest by, if it has something to say
     */
    record Settled(Durable message, Optional<String> report) implements Message {
    }

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

    /**
     * What came back of a photo the pet asked its owner's game for. Its durable part is the shot on the
     * film roll and the biome among those the pet has photographed, see {@code PhotoResults}.
     */
    sealed interface PhotoResult extends Durable {
        /** @return the camera item the photo was taken with, which the rest after it depends on */
        ResourceLocation camera();

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
