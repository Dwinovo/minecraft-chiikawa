package com.dwinovo.chiikawa.entity.brain.errand.work;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.data.WorkData;
import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.entity.brain.constraint.PetOwnership;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandTunings;
import com.dwinovo.chiikawa.entity.brain.errand.view.DayPhase;
import com.dwinovo.chiikawa.entity.brain.errand.work.action.BuskAction;
import com.dwinovo.chiikawa.task.PetTask;
import com.dwinovo.chiikawa.testing.TestAnchors;
import com.dwinovo.chiikawa.testing.TestErrands;
import com.dwinovo.chiikawa.testing.TestView;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** What a work errand does with the work it is made from: its requirements first, then its action. */
class WorkErrandTest {
    private static final ResourceKey<Level> OVERWORLD = ResourceKey.create(
        ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("dimension")),
        ResourceLocation.withDefaultNamespace("overworld"));
    private static final GlobalPos HOME = GlobalPos.of(OVERWORLD, new BlockPos(0, 64, 0));
    private static final PetAnchor FREE = TestAnchors.free(HOME);
    private static final ErrandTunings.PlayMusic TUNING = new ErrandTunings.PlayMusic(50);

    @BeforeAll
    static void shippedSettings() {
        TestErrands.load();
    }

    @Test
    void theErrandIsOfTheCategoryAndCounterTheWorkSays() {
        WorkErrand<BuskAction.Performance, ErrandTunings.PlayMusic> errand = errand(WorkRequirement.AT_NIGHT);

        assertEquals(ErrandCategory.WORK, errand.category());
        assertEquals(Optional.of(WorkData.PLAY_MUSIC_SECOND), errand.workCounter());
    }

    @Test
    void aRequirementComesBeforeTheActionAndIsAskedAgainWhileTheWorkGoesOn() {
        WorkErrand<BuskAction.Performance, ErrandTunings.PlayMusic> errand = errand(WorkRequirement.AT_NIGHT);
        TestView night = TestView.at(HOME, FREE).phase(DayPhase.NIGHT).withSong();
        TestView day = TestView.at(HOME, FREE).phase(DayPhase.DAY).withSong();

        assertFalse(errand.offers(night, TUNING).isEmpty());
        // The song is there but the night is not: the requirement answers before the action is asked.
        assertEquals("intent.chiikawa.fail.not_night", errand.offers(day, TUNING).check().reasonKey());
        BuskAction.Performance plan = errand.offers(night, TUNING).list().get(0).plan();
        assertTrue(errand.stillWanted(plan, night, TUNING).ok());
        assertEquals("intent.chiikawa.fail.not_night", errand.stillWanted(plan, day, TUNING).reasonKey());
    }

    @Test
    void anOwnedPetDoesTheWorkOnlyOnASlipThatCountsItAndAWildPetOnItsOwn() {
        WorkErrand<BuskAction.Performance, ErrandTunings.PlayMusic> errand = errand(WorkRequirement.WILD_OR_SLIP);
        TestView owned = TestView.at(HOME, FREE).ownership(TestView.OWNED).withSong();

        assertEquals("intent.chiikawa.fail.needs_slip", errand.offers(owned, TUNING).check().reasonKey());
        assertEquals("intent.chiikawa.fail.needs_slip",
            errand.offers(owned.slip(slip(ResourceLocation.fromNamespaceAndPath("chiikawa", "weed"))), TUNING).check().reasonKey());
        assertFalse(errand.offers(owned.slip(slip(WorkData.PLAY_MUSIC_SECOND)), TUNING).isEmpty());
        assertFalse(errand.offers(TestView.at(HOME, TestAnchors.wild(HOME)).ownership(PetOwnership.WILD).withSong(), TUNING)
            .isEmpty());
    }

    @Test
    void aSlipThatCountsTheWorkHasTheSongPlayedOverWhateverTheBoxSaysNext() {
        TestView slip = TestView.at(HOME, FREE).withSong().slip(slip(WorkData.PLAY_MUSIC_SECOND));
        TestView none = TestView.at(HOME, FREE).withSong();

        assertTrue(TestErrands.planOf(WorkData.PLAY_MUSIC, slip) instanceof BuskAction.Performance performance
            && performance.song().onSlip());
        assertTrue(TestErrands.planOf(WorkData.PLAY_MUSIC, none) instanceof BuskAction.Performance performance
            && !performance.song().onSlip());
    }

    private static WorkErrand<BuskAction.Performance, ErrandTunings.PlayMusic> errand(WorkRequirement... requires) {
        return new WorkErrand<>(WorkData.PLAY_MUSIC, new Work<>(WorkActions.BUSK, ErrandCategory.WORK,
            Optional.of(WorkData.PLAY_MUSIC_SECOND), List.of(requires), Optional.empty(), Optional.empty()));
    }

    private static PetTask slip(ResourceLocation counter) {
        return new PetTask(ResourceLocation.fromNamespaceAndPath("chiikawa", "test"),
            ResourceLocation.fromNamespaceAndPath("chiikawa", "musician"), counter, PetTask.NO_ICON, 10,
            ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath("chiikawa", "pet_task/test")), 0);
    }
}
