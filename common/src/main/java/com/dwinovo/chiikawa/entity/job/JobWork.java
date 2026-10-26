package com.dwinovo.chiikawa.entity.job;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandType;
import com.dwinovo.chiikawa.entity.brain.errand.Errands;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkErrand;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
 * @param sight how the pet looks for the blocks its work is done to; a job whose work is done to blocks must have it
 */
public record JobWork(List<ResourceLocation> work, Optional<Sight> sight) {
    public static final Codec<JobWork> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ResourceLocation.CODEC.listOf().fieldOf("work").forGetter(JobWork::work),
        Sight.CODEC.optionalFieldOf("sight").forGetter(JobWork::sight)
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

    /**
     * Whether the pet is ready for the work its job does that counts this counter, see
     * {@link com.dwinovo.chiikawa.entity.brain.errand.work.WorkAction#readyFor}: the one answer to whether a slip of that
     * counter is worth the pet taking. A counter no work of the job counts, such as a hunting slip's, asks nothing of the
     * pet. Pure.
     *
     * @param pet a pet
     * @param view what the pet perceives
     * @param counter the work counter of a slip
     */
    public static boolean readyFor(AbstractPet pet, PetView view, ResourceLocation counter) {
        List<WorkErrand<?, ?>> counting = errandsOf(pet).stream()
            .filter(WorkErrand.class::isInstance).<WorkErrand<?, ?>>map(WorkErrand.class::cast)
            .filter(work -> work.counts(counter))
            .toList();
        return counting.isEmpty() || counting.stream().anyMatch(work -> work.readyFor(view));
    }

    /** @return how the pet looks for the blocks its job's work is done to, if its job says */
    public static Optional<Sight> sightOf(AbstractPet pet) {
        return Optional.ofNullable(byJob.get(pet.getCapabilityId())).flatMap(JobWork::sight);
    }

    /** Replaces them all, as a data pack reload does. Public so an in-game case can try other work. */
    public static void replaceAll(Map<ResourceLocation, JobWork> work) {
        byJob = Map.copyOf(work);
        REPORTED.clear();
    }
}
