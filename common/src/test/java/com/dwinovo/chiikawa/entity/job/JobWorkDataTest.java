package com.dwinovo.chiikawa.entity.job;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dwinovo.chiikawa.data.JobWorkData;
import com.dwinovo.chiikawa.data.WorkData;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/** What the generated job files say: the work each job has always had, in the order it has always been listed. */
class JobWorkDataTest {
    @Test
    void eachJobHasTheWorkItHadWhenTheCodeListedIt() {
        Map<ResourceLocation, List<ResourceLocation>> expected = Map.of(
            JobIds.FARMER, List.of(ErrandIds.HARVEST, ErrandIds.PLANT, ErrandIds.DELIVER, ErrandIds.WEED, ErrandIds.PICK_MUSHROOM),
            JobIds.FENCER, List.of(ErrandIds.MELEE),
            JobIds.ARCHER, List.of(ErrandIds.RANGED),
            JobIds.MUSICIAN, List.of(WorkData.PLAY_MUSIC));

        assertEquals(expected.keySet(), JobWorkData.all().keySet(), "a pet with no job has no file");
        expected.forEach((job, work) -> assertEquals(work, JobWorkData.all().get(job).work(), job.toString()));
    }

    @Test
    void workIsWrittenAsAListInOrderAndReadBackTheSame() {
        JobWorkData.all().forEach((job, work) -> {
            var written = JobWork.CODEC.encodeStart(JsonOps.INSTANCE, work).getOrThrow();

            assertEquals(work.work().size(), written.getAsJsonObject().getAsJsonArray("work").size(), job.toString());
            assertEquals(work, JobWork.CODEC.parse(JsonOps.INSTANCE, written).getOrThrow(), job.toString());
        });
    }
}
