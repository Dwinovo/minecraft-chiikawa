package com.dwinovo.chiikawa.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandType;
import com.dwinovo.chiikawa.entity.brain.errand.Errands;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkErrand;
import com.dwinovo.chiikawa.entity.job.JobWork;
import com.dwinovo.chiikawa.task.PetTaskType;
import com.dwinovo.chiikawa.task.PetWorkCounters;
import com.dwinovo.chiikawa.testing.TestErrands;
import com.mojang.serialization.JsonOps;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * That the data the mod ships holds together, which no loader can say of one file alone: the errands, the work of
 * every job and the slips that count the work. Read as a game reads it, from what the data generator writes.
 */
class WorkDataTest {
    @BeforeAll
    static void shippedData() {
        TestErrands.load();
    }

    @Test
    void settingsFilesAndErrandsAreTheSameIds() {
        Set<ResourceLocation> errands = Errands.all().stream().map(ErrandType::id).collect(Collectors.toSet());
        Set<ResourceLocation> works = WorkData.all().keySet();

        assertEquals(38, errands.size());
        assertEquals(errands, ErrandSettingsData.all().keySet(), "settings and errands are each other's");
        assertEquals(errands, ErrandSettings.all().keySet(), "the loader kept every file");
        assertEquals(errands, ErrandFiles.all().keySet(), "every errand has a file");
        assertTrue(errands.containsAll(works), "a work is an errand");
        errands.forEach(errand -> assertEquals(works.contains(errand), !Errands.isCode(errand),
            errand + " is a work exactly when it is not written in code"));
    }

    @Test
    void everyWorkIsAWorkErrandThatItsActionAcceptsTheTargetOf() {
        WorkData.all().forEach((id, work) -> {
            assertInstanceOf(WorkErrand.class, Errands.get(id), id.toString());
            assertEquals(Optional.empty(), work.action().refuses(work.target()), id.toString());
        });
    }

    @Test
    void everyJobsWorkIsWorkErrandsInTheOrderTheJobIsShippedWith() {
        JobWorkData.all().forEach((job, shipped) -> {
            JobWork read = JobWork.CODEC.parse(JsonOps.INSTANCE, JobWork.CODEC.encodeStart(JsonOps.INSTANCE, shipped).getOrThrow())
                .getOrThrow();

            assertEquals(shipped.work(), read.work(), job + " keeps its order");
            assertEquals(read.work().size(), new HashSet<>(read.work()).size(), job + " names an errand twice");
            read.work().forEach(errand -> {
                assertFalse(Errands.isCode(errand), job + " lists " + errand + ", which is written in code");
                assertInstanceOf(WorkErrand.class, Errands.get(errand), job + " lists " + errand);
            });
        });
    }

    @Test
    void aJobHasASightExactlyWhenItsWorkIsDoneToBlocks() {
        JobWorkData.all().forEach((job, work) -> {
            boolean toBlocks = work.work().stream().anyMatch(errand -> ((WorkErrand<?, ?>) Errands.get(errand)).blocks().isPresent());

            assertEquals(toBlocks, work.sight().isPresent(), job.toString());
        });
    }

    @Test
    void everySlipCountsAWorksCounterAndItsJobDoesTheWork() {
        Set<ResourceLocation> counters = WorkData.all().values().stream().flatMap(work -> work.counter().stream())
            .collect(Collectors.toSet());

        PetTaskTypeData.all().forEach((slip, type) -> {
            assertTrue(counters.contains(type.counter()), slip + " counts " + type.counter() + ", which no work reports");
            assertTrue(countedByTheJobOf(type), slip + " is for " + type.capability() + ", whose work never reports " + type.counter());
        });
    }

    @Test
    void theSlayIsTheCounterOfTheHuntsAndOfNoOtherWork() {
        Set<ResourceLocation> hunts = WorkData.all().entrySet().stream()
            .filter(work -> work.getValue().counter().equals(Optional.of(PetWorkCounters.SLAY)))
            .map(Map.Entry::getKey).collect(Collectors.toSet());

        assertEquals(Set.of(WorkData.HUNT_MELEE, WorkData.HUNT_RANGED), hunts);
    }

    private static boolean countedByTheJobOf(PetTaskType type) {
        List<ResourceLocation> work = JobWorkData.all().get(type.capability()).work();
        return work.stream().anyMatch(errand -> WorkData.all().get(errand).counter().filter(type.counter()::equals).isPresent());
    }
}
