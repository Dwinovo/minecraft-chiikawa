package com.dwinovo.chiikawa.entity.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.data.WorkData;
import com.google.gson.JsonParser;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** What the loader does with a job file that is wrong: it costs that job its work and nothing else. */
class JobWorkLoaderTest {
    private static final Set<ResourceLocation> JOBS = Set.of(JobIds.FARMER, JobIds.FENCER);
    private static final Set<ResourceLocation> ERRANDS = Set.of(WorkData.HARVEST, WorkData.PLANT, WorkData.MELEE);
    private static final Set<ResourceLocation> BLOCK_WORKS = Set.of(WorkData.HARVEST, WorkData.PLANT);
    private static final Sight FIELD = new Sight(5, 1, 10.0, 30, 3);
    private static final String SIGHT = "\"sight\": { \"radius\": 5, \"vertical_range\": 1, \"forget_distance\": 10.0,"
        + " \"backoff_step_ticks\": 30, \"empty_streak_limit\": 3 }";

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void aFileIsReadAsAnOrderedListOfErrandIds() {
        JobWorkLoader.Loaded loaded = JobWorkLoader.load(Map.of(
            JobIds.FARMER, JsonParser.parseString("{ \"work\": [\"chiikawa:plant\", \"chiikawa:harvest\"], " + SIGHT + " }")),
            JOBS, ERRANDS, BLOCK_WORKS);

        assertEquals(Map.of(JobIds.FARMER, new JobWork(List.of(WorkData.PLANT, WorkData.HARVEST), Optional.of(FIELD))),
            loaded.work());
        assertTrue(loaded.errors().isEmpty(), loaded.errors()::toString);
        assertTrue(loaded.warnings().isEmpty(), loaded.warnings()::toString);
    }

    @Test
    void aFileNamedForNoJobIsWarnedAboutAndSkipped() {
        ResourceLocation stranger = ResourceLocation.fromNamespaceAndPath("chiikawa", "baker");
        JobWorkLoader.Loaded loaded = JobWorkLoader.load(Map.of(
            JobIds.FENCER, JsonParser.parseString("{ \"work\": [\"chiikawa:melee\"] }"),
            stranger, JsonParser.parseString("{ \"work\": [\"chiikawa:harvest\"] }")), JOBS, ERRANDS, BLOCK_WORKS);

        assertEquals(Set.of(JobIds.FENCER), loaded.work().keySet());
        assertTrue(loaded.errors().isEmpty(), loaded.errors()::toString);
        assertEquals(1, loaded.warnings().size(), loaded.warnings()::toString);
        assertTrue(loaded.warnings().get(0).contains("chiikawa:baker"), loaded.warnings()::toString);
    }

    @Test
    void anErrandThatNoPackHasIsWarnedAboutAndTheWorkKept() {
        JobWorkLoader.Loaded loaded = JobWorkLoader.load(Map.of(
            JobIds.FARMER, JsonParser.parseString("{ \"work\": [\"chiikawa:harvest\", \"chiikawa:sulk\"], " + SIGHT + " }")),
            JOBS, ERRANDS, BLOCK_WORKS);

        assertEquals(Map.of(JobIds.FARMER, new JobWork(List.of(WorkData.HARVEST,
            ResourceLocation.fromNamespaceAndPath("chiikawa", "sulk")), Optional.of(FIELD))), loaded.work());
        assertTrue(loaded.errors().isEmpty(), loaded.errors()::toString);
        assertEquals(1, loaded.warnings().size(), loaded.warnings()::toString);
        assertTrue(loaded.warnings().get(0).contains("chiikawa:sulk") && loaded.warnings().get(0).contains("chiikawa:farmer"),
            loaded.warnings()::toString);
    }

    @Test
    void aFileThatDoesNotParseCostsOnlyItsOwnJobItsWork() {
        JobWorkLoader.Loaded loaded = JobWorkLoader.load(Map.of(
            JobIds.FARMER, JsonParser.parseString("{ \"work\": \"harvest\" }"),
            JobIds.FENCER, JsonParser.parseString("{ \"work\": [\"chiikawa:melee\"] }")), JOBS, ERRANDS, BLOCK_WORKS);

        assertEquals(Set.of(JobIds.FENCER), loaded.work().keySet());
        assertEquals(1, loaded.errors().size(), loaded.errors()::toString);
        assertTrue(loaded.errors().get(0).contains("chiikawa:farmer"), loaded.errors()::toString);
    }

    @Test
    void aJobWithWorkDoneToBlocksAndNoSightOffersNoWork() {
        JobWorkLoader.Loaded loaded = JobWorkLoader.load(Map.of(
            JobIds.FARMER, JsonParser.parseString("{ \"work\": [\"chiikawa:harvest\", \"chiikawa:plant\"] }"),
            JobIds.FENCER, JsonParser.parseString("{ \"work\": [\"chiikawa:melee\"] }")), JOBS, ERRANDS, BLOCK_WORKS);

        assertEquals(Set.of(JobIds.FENCER), loaded.work().keySet());
        assertEquals(1, loaded.errors().size(), loaded.errors()::toString);
        assertTrue(loaded.errors().get(0).contains("chiikawa:farmer") && loaded.errors().get(0).contains("sight"),
            loaded.errors()::toString);
    }

    @Test
    void aJobWithNoWorkDoneToBlocksNeedsNoSightAndMayHaveOne() {
        JobWorkLoader.Loaded loaded = JobWorkLoader.load(Map.of(
            JobIds.FENCER, JsonParser.parseString("{ \"work\": [\"chiikawa:melee\"], " + SIGHT + " }")), JOBS, ERRANDS, BLOCK_WORKS);

        assertEquals(Map.of(JobIds.FENCER, new JobWork(List.of(WorkData.MELEE), Optional.of(FIELD))), loaded.work());
        assertTrue(loaded.errors().isEmpty(), loaded.errors()::toString);
    }

    @Test
    void aSightWithoutAFieldOrWithANumberOutOfRangeCostsItsJobItsWork() {
        for (String broken : new String[] {
            "\"sight\": { \"radius\": 5, \"vertical_range\": 1, \"forget_distance\": 10.0, \"backoff_step_ticks\": 30 }",
            "\"sight\": { \"radius\": -1, \"vertical_range\": 1, \"forget_distance\": 10.0, \"backoff_step_ticks\": 30,"
                + " \"empty_streak_limit\": 3 }",
            "\"sight\": { \"radius\": 5, \"vertical_range\": 1, \"forget_distance\": 10.0, \"backoff_step_ticks\": 0,"
                + " \"empty_streak_limit\": 3 }"}) {
            JobWorkLoader.Loaded loaded = JobWorkLoader.load(Map.of(
                JobIds.FARMER, JsonParser.parseString("{ \"work\": [\"chiikawa:harvest\"], " + broken + " }")),
                JOBS, ERRANDS, BLOCK_WORKS);

            assertTrue(loaded.work().isEmpty(), broken);
            assertEquals(1, loaded.errors().size(), loaded.errors()::toString);
        }
    }
}
