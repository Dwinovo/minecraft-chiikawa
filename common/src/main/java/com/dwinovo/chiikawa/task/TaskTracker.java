package com.dwinovo.chiikawa.task;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.PetMind;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.Cue;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.Message;
import net.minecraft.resources.ResourceLocation;

/**
 * Counts the work a pet reports against the slip it carries, and has the slip paid out the
 * moment it is finished: wherever the pet is, the reward goes straight into its backpack
 * with a sparkle. The pet is told it is paid and does the rest itself, see {@link SlipWages}.
 */
public final class TaskTracker {
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

    /**
     * The slip is let go of at once, so work reported in the same tick is not counted against it twice. Glad of
     * its pay at once, whatever its mind is up to, a pet that cannot decide for itself included.
     */
    private static void complete(AbstractPet pet, PetTask task) {
        pet.setTask(null);
        PetMind.post(pet, new Message.SlipCompleted(task));
        PetMind.cue(pet, new Cue.Paid());
    }
}
