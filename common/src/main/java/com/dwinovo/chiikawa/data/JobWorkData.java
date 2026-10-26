package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.job.JobIds;
import com.dwinovo.chiikawa.entity.job.JobWork;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * The generated work of each job, by the id of the job, in the order a pet weighs the errands in: the farmer harvests,
 * plants and delivers before it weeds and picks mushrooms. A pet with no job has no file, and does only what every
 * pet does.
 */
public final class JobWorkData {
    private JobWorkData() {
    }

    /** @return work by job id */
    public static Map<ResourceLocation, JobWork> all() {
        return Map.of(
            JobIds.FARMER, new JobWork(List.of(ErrandIds.HARVEST, ErrandIds.PLANT, ErrandIds.DELIVER, ErrandIds.WEED,
                ErrandIds.PICK_MUSHROOM)),
            JobIds.FENCER, new JobWork(List.of(ErrandIds.MELEE)),
            JobIds.ARCHER, new JobWork(List.of(ErrandIds.RANGED)),
            JobIds.MUSICIAN, new JobWork(List.of(ErrandIds.PLAY_MUSIC)));
    }
}
