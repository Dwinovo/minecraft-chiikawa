package com.dwinovo.chiikawa.client.ui;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.task.PetTask;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * What a pet is doing, in words. One place builds these lines, so the backpack screen,
 * the label above a pet's head and the labor board all say the same thing.
 */
public final class PetStatusText {
    private PetStatusText() {
    }

    /** "Doing: pulling weeds", or that it is idle. */
    public static Component doing(AbstractPet pet) {
        return pet.getIntent()
            .<Component>map(intent -> Component.translatable("screen.chiikawa.pet.doing", intentName(intent)))
            .orElseGet(() -> Component.translatable("screen.chiikawa.pet.doing.nothing"));
    }

    /** "Slip: weeding 3/8", or that it carries none. */
    public static Component slip(AbstractPet pet) {
        return pet.getTask()
            .<Component>map(task -> Component.translatable("screen.chiikawa.pet.slip", slipProgress(task)))
            .orElseGet(() -> Component.translatable("screen.chiikawa.pet.slip.none"));
    }

    /**
     * The one line that floats over a working pet: what it is doing, and the slip it is
     * doing it for.
     *
     * @return nothing while the pet has neither, so idle pets stay quiet
     */
    public static Optional<Component> label(AbstractPet pet) {
        Optional<Component> intent = pet.getIntent().map(PetStatusText::intentName);
        Optional<Component> progress = pet.getTask().map(PetStatusText::slipProgress);
        if (intent.isEmpty()) {
            return progress;
        }
        return Optional.of(progress
            .<Component>map(slip -> Component.translatable("screen.chiikawa.pet.label", intent.get(), slip))
            .orElseGet(intent::get));
    }

    /** "Weeding 3/8". */
    public static Component slipProgress(PetTask task) {
        return Component.translatable("screen.chiikawa.pet.slip.short", taskName(task.type()), task.progress(), task.target());
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
