package com.dwinovo.chiikawa.client.ui;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.task.PetTask;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * What a pet is doing, in words. One place builds these, so the backpack screen and the
 * labor board say the same thing about the same pet.
 *
 * <p>Short by design: each of these is read beside an icon, a bar or a badge that already
 * carries half the meaning. The sentences that used to be here — "Doing: X", "Slip: Y
 * 3/8" — were a label and its value written out, which is the one shape a reader has to
 * read word by word instead of taking in at a glance.
 */
public final class PetStatusText {
    private PetStatusText() {
    }

    /**
     * What the pet is at, in a word or two: the intent it is running, else the slip it
     * carries, else that it is idle.
     */
    public static Component activity(AbstractPet pet) {
        return pet.getIntent()
            .map(PetStatusText::intentName)
            .or(() -> pet.getTask().map(task -> taskName(task.type())))
            .orElseGet(() -> Component.translatable("screen.chiikawa.pet.doing.nothing"));
    }

    /** How far along a slip is, as a count. Numbers read the same in every language. */
    public static Component slipCount(PetTask task) {
        return Component.literal(task.progress() + "/" + task.target());
    }

    /** What a slip type is called. */
    public static Component taskName(ResourceLocation type) {
        return Component.translatable("pet_task." + type.getNamespace() + "." + type.getPath());
    }

    /** How much work a slip asks for, in that type's own unit: "12 weeds", "180s". */
    public static Component taskAmount(ResourceLocation type, int amount) {
        return Component.translatable("pet_task." + type.getNamespace() + "." + type.getPath() + ".amount", amount);
    }

    /** What a job is called; the same name the backpack tooltip uses. */
    public static Component jobName(ResourceLocation capability) {
        return Component.translatable("tooltip." + capability.getNamespace() + ".pet_job." + capability.getPath());
    }

    private static Component intentName(ResourceLocation intent) {
        return Component.translatable("intent." + intent.getNamespace() + "." + intent.getPath());
    }
}
