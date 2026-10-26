package com.dwinovo.chiikawa.entity.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.data.WorkData;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.google.gson.JsonParser;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** What the loader does with a job file that is wrong: it costs that job its work and nothing else. */
class JobWorkLoaderTest {
    private static final Set<ResourceLocation> JOBS = Set.of(JobIds.FARMER, JobIds.FENCER);
    private static final Set<ResourceLocation> ERRANDS = Set.of(ErrandIds.HARVEST, ErrandIds.PLANT, WorkData.MELEE);

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void aFileIsReadAsAnOrderedListOfErrandIds() {
        JobWorkLoader.Loaded loaded = JobWorkLoader.load(Map.of(
            JobIds.FARMER, JsonParser.parseString("{ \"work\": [\"chiikawa:plant\", \"chiikawa:harvest\"] }")), JOBS, ERRANDS);

        assertEquals(Map.of(JobIds.FARMER, new JobWork(List.of(ErrandIds.PLANT, ErrandIds.HARVEST))), loaded.work());
        assertTrue(loaded.errors().isEmpty(), loaded.errors()::toString);
        assertTrue(loaded.warnings().isEmpty(), loaded.warnings()::toString);
    }

    @Test
    void aFileNamedForNoJobIsWarnedAboutAndSkipped() {
        ResourceLocation stranger = ResourceLocation.fromNamespaceAndPath("chiikawa", "baker");
        JobWorkLoader.Loaded loaded = JobWorkLoader.load(Map.of(
            JobIds.FENCER, JsonParser.parseString("{ \"work\": [\"chiikawa:melee\"] }"),
            stranger, JsonParser.parseString("{ \"work\": [\"chiikawa:harvest\"] }")), JOBS, ERRANDS);

        assertEquals(Set.of(JobIds.FENCER), loaded.work().keySet());
        assertTrue(loaded.errors().isEmpty(), loaded.errors()::toString);
        assertEquals(1, loaded.warnings().size(), loaded.warnings()::toString);
        assertTrue(loaded.warnings().get(0).contains("chiikawa:baker"), loaded.warnings()::toString);
    }

    @Test
    void anErrandThatNoPackHasIsWarnedAboutAndTheWorkKept() {
        JobWorkLoader.Loaded loaded = JobWorkLoader.load(Map.of(
            JobIds.FARMER, JsonParser.parseString("{ \"work\": [\"chiikawa:harvest\", \"chiikawa:sulk\"] }")), JOBS, ERRANDS);

        assertEquals(Map.of(JobIds.FARMER, new JobWork(List.of(ErrandIds.HARVEST,
            ResourceLocation.fromNamespaceAndPath("chiikawa", "sulk")))), loaded.work());
        assertTrue(loaded.errors().isEmpty(), loaded.errors()::toString);
        assertEquals(1, loaded.warnings().size(), loaded.warnings()::toString);
        assertTrue(loaded.warnings().get(0).contains("chiikawa:sulk") && loaded.warnings().get(0).contains("chiikawa:farmer"),
            loaded.warnings()::toString);
    }

    @Test
    void aFileThatDoesNotParseCostsOnlyItsOwnJobItsWork() {
        JobWorkLoader.Loaded loaded = JobWorkLoader.load(Map.of(
            JobIds.FARMER, JsonParser.parseString("{ \"work\": \"harvest\" }"),
            JobIds.FENCER, JsonParser.parseString("{ \"work\": [\"chiikawa:melee\"] }")), JOBS, ERRANDS);

        assertEquals(Set.of(JobIds.FENCER), loaded.work().keySet());
        assertEquals(1, loaded.errors().size(), loaded.errors()::toString);
        assertTrue(loaded.errors().get(0).contains("chiikawa:farmer"), loaded.errors()::toString);
    }
}
