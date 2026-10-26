package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.entity.job.JobIds;
import com.dwinovo.chiikawa.entity.job.JobWork;
import com.dwinovo.chiikawa.entity.job.Sight;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * The generated work of each job, by the id of the job, in the order a pet weighs the errands in: the farmer harvests,
 * plants and delivers before it weeds and picks mushrooms. A job whose work is done to blocks says how far and how often
 * its pet looks for them. A pet with no job has no file, and does only what every pet does.
 */
public final class JobWorkData {
    private JobWorkData() {
    }

    /** @return work by job id */
    public static Map<ResourceLocation, JobWork> all() {
        // A farmer looks five blocks round and one up and down, forgets what it has seen ten blocks away, and for each
        // search in a row that found nothing waits a second and a half longer, up to three times that.
        Sight field = new Sight(5, 1, 10.0, 30, 3);
        return Map.of(
            JobIds.FARMER, new JobWork(List.of(WorkData.HARVEST, WorkData.PLANT, WorkData.DELIVER, WorkData.WEED,
                WorkData.PICK_MUSHROOM), Optional.of(field)),
            JobIds.FENCER, new JobWork(List.of(WorkData.MELEE, WorkData.HUNT_MELEE), Optional.empty()),
            JobIds.ARCHER, new JobWork(List.of(WorkData.RANGED, WorkData.HUNT_RANGED), Optional.empty()),
            JobIds.MUSICIAN, new JobWork(List.of(WorkData.PLAY_MUSIC), Optional.empty()));
    }
}
