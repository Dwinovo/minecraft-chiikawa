package com.dwinovo.chiikawa.task;

import com.dwinovo.chiikawa.utils.ModCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;

/**
 * A slip (工作牌): one job's worth of a kind of work, rolled from a {@link PetTaskType}
 * onto a labor board and then carried by the pet that claimed it. It holds everything it
 * needs to be finished and paid, so a data pack reload cannot strand a slip a pet carries.
 *
 * @param type the task type it was rolled from
 * @param capability the capability (job) that can take it
 * @param counter the work counter that advances it, see {@link PetWorkCounters}
 * @param icon the item this work is pictured as, copied from the type like everything else
 *             here, so a label over a pet needs no data pack of its own to draw it
 * @param target how much of that work finishes it
 * @param reward loot table rolled into the pet's backpack once it is finished
 * @param progress work done so far
 */
public record PetTask(
    ResourceLocation type,
    ResourceLocation capability,
    ResourceLocation counter,
    ResourceLocation icon,
    int target,
    ResourceLocation reward,
    int progress
) {
    /** What a slip written before slips had a picture, or one naming no item, shows: nothing. */
    public static final ResourceLocation NO_ICON = new ResourceLocation("air");

    public static final Codec<PetTask> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ResourceLocation.CODEC.fieldOf("type").forGetter(PetTask::type),
        ResourceLocation.CODEC.fieldOf("capability").forGetter(PetTask::capability),
        ResourceLocation.CODEC.fieldOf("counter").forGetter(PetTask::counter),
        ModCodecs.strictOptionalField(ResourceLocation.CODEC, "icon", NO_ICON).forGetter(PetTask::icon),
        ExtraCodecs.POSITIVE_INT.fieldOf("target").forGetter(PetTask::target),
        ResourceLocation.CODEC.fieldOf("reward").forGetter(PetTask::reward),
        ModCodecs.strictOptionalField(ExtraCodecs.NON_NEGATIVE_INT, "progress", 0).forGetter(PetTask::progress)
    ).apply(instance, PetTask::new));

    /**
     * @param amount work just done
     * @return this slip with that work counted, never past its target
     */
    public PetTask advance(int amount) {
        return new PetTask(type, capability, counter, icon, target, reward, Math.min(target, progress + amount));
    }

    public boolean isDone() {
        return progress >= target;
    }
}
