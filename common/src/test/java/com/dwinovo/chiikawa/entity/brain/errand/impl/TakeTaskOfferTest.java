package com.dwinovo.chiikawa.entity.brain.errand.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandTunings;
import com.dwinovo.chiikawa.task.BoardSlips;
import com.dwinovo.chiikawa.task.PetTask;
import com.dwinovo.chiikawa.task.PetWorkCounters;
import com.dwinovo.chiikawa.testing.TestAnchors;
import com.dwinovo.chiikawa.testing.TestErrands;
import com.dwinovo.chiikawa.testing.TestView;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TakeTaskOfferTest {
    private static final ResourceKey<Level> OVERWORLD = ResourceKey.create(
        ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("dimension")),
        ResourceLocation.withDefaultNamespace("overworld"));
    private static final GlobalPos HOME = GlobalPos.of(OVERWORLD, new BlockPos(0, 64, 0));
    private static final PetAnchor FREE = TestAnchors.free(HOME);

    @BeforeAll
    static void shippedSettings() {
        TestErrands.load();
    }

    @Test
    void takesASlipFromABoardAndGoesToIt() {
        TestView view = boardAt(10);

        assertEquals(ErrandCategory.TAKE_TASK, TestErrands.errand(ErrandIds.TAKE_TASK).category());
        assertEquals(1, TestErrands.offers(ErrandIds.TAKE_TASK, view).size());
        assertEquals(Optional.of(GlobalPos.of(OVERWORLD, HOME.pos().east(10))),
            TestErrands.planOf(ErrandIds.TAKE_TASK, view).destination(view));
    }

    @Test
    void carriesOneSlipAtATime() {
        PetTask slip = new PetTask(ResourceLocation.fromNamespaceAndPath("chiikawa", "weeding"),
            ResourceLocation.fromNamespaceAndPath("chiikawa", "farmer"), PetWorkCounters.WEED, PetTask.NO_ICON, 8,
            ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath("chiikawa", "pet_task/weeding")), 0);

        assertEquals("intent.chiikawa.fail.has_slip", TestErrands.whyNoOffer(ErrandIds.TAKE_TASK, boardAt(1).slip(slip)).reasonKey());
        // Having taken it ends the walk to the board.
        assertEquals("intent.chiikawa.fail.has_slip",
            TestErrands.stillWanted(ErrandIds.TAKE_TASK, trip(1), boardAt(1).slip(slip)).reasonKey());
    }

    @Test
    void needsABoardOfferingOneAndGoesOnOnlyWhileThereIsOne() {
        assertTrue(TestErrands.offers(ErrandIds.TAKE_TASK, TestView.at(HOME, FREE)).isEmpty());
        assertEquals("intent.chiikawa.fail.no_slip", TestErrands.whyNoOffer(ErrandIds.TAKE_TASK, TestView.at(HOME, FREE)).reasonKey());
        assertTrue(TestErrands.stillWanted(ErrandIds.TAKE_TASK, trip(1), boardAt(1)).ok());
        assertEquals("intent.chiikawa.fail.no_slip",
            TestErrands.stillWanted(ErrandIds.TAKE_TASK, trip(1), TestView.at(HOME, FREE)).reasonKey());
    }

    @Test
    void theHoldOnASlipOutlastsTheErrandThatMadeIt() {
        ErrandSettings<ErrandTunings.TakeTask> shipped = settings();
        TakeTaskErrand errand = new TakeTaskErrand();

        assertEquals(Optional.empty(), errand.problem(shipped));
        assertEquals(Optional.empty(), errand.problem(withLimit(shipped, BoardSlips.RESERVATION_TICKS)));
        assertTrue(errand.problem(withLimit(shipped, BoardSlips.RESERVATION_TICKS + 1)).isPresent());
    }

    private static TakeTaskErrand.Trip trip(int blocksEast) {
        return new TakeTaskErrand.Trip(GlobalPos.of(OVERWORLD, HOME.pos().east(blocksEast)));
    }

    @SuppressWarnings("unchecked")
    private static ErrandSettings<ErrandTunings.TakeTask> settings() {
        return (ErrandSettings<ErrandTunings.TakeTask>) ErrandSettings.of(ErrandIds.TAKE_TASK).orElseThrow();
    }

    private static ErrandSettings<ErrandTunings.TakeTask> withLimit(ErrandSettings<ErrandTunings.TakeTask> settings, int maxTicks) {
        return new ErrandSettings<>(settings.priority(), settings.score(), maxTicks, settings.rests(), settings.yieldsTo(),
            settings.overlays(), settings.startVoice(), settings.tuning());
    }

    private static TestView boardAt(int blocksEast) {
        return TestView.at(HOME, FREE).offeringBoard(GlobalPos.of(OVERWORLD, HOME.pos().east(blocksEast)));
    }
}
