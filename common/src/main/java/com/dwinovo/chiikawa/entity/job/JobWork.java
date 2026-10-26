package com.dwinovo.chiikawa.entity.job;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandType;
import com.dwinovo.chiikawa.entity.brain.errand.Errands;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;

/**
 * The work of a job, loaded from {@code data/<namespace>/pet_job/<job>.json} by {@link JobWorkLoader}: one file per
 * job, named by the job's id, so a pack can give a job other work.
 *
 * <p>Server-side only: a player's game is told what a pet is doing, not what it could be doing.
 *
 * @param work the errands the job adds to the ones every pet considers, in the order the pet weighs them, which
 *             settles a tie between errands that are otherwise equal
 */
public record JobWork(List<ResourceLocation> work) {
    public static final Codec<JobWork> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ResourceLocation.CODEC.listOf().fieldOf("work").forGetter(JobWork::work)
    ).apply(instance, JobWork::new));

    private static final String LOG_PREFIX = "[chiikawa-job] ";
    private static volatile Map<ResourceLocation, JobWork> byJob = Collections.emptyMap();
    private static final Set<ResourceLocation> REPORTED = ConcurrentHashMap.newKeySet();

    /**
     * @param pet a pet
     * @return the errands the pet considers: those every pet does, then those of its job's work; an errand the work
     *         names that nothing registers is left out, and said so once
     */
    public static List<ErrandType<?, ?>> errandsOf(AbstractPet pet) {
        List<ErrandType<?, ?>> errands = new ArrayList<>(Errands.generic());
        JobWork work = byJob.get(pet.getCapabilityId());
        if (work != null) {
            for (ResourceLocation id : work.work) {
                ErrandType<?, ?> type = Errands.get(id);
                if (type != null) {
                    errands.add(type);
                } else if (REPORTED.add(id)) {
                    Constants.LOG.error(LOG_PREFIX + id + " is left out of the work of " + pet.getCapabilityId()
                        + ", there is no such errand");
                }
            }
        }
        return errands;
    }

    /** Replaces them all, as a data pack reload does. Public so an in-game case can try other work. */
    public static void replaceAll(Map<ResourceLocation, JobWork> work) {
        byJob = Map.copyOf(work);
        REPORTED.clear();
    }
}
