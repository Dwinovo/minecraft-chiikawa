package com.dwinovo.chiikawa.entity.brain.errand.work;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.data.WorkData;
import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.testing.TestAnchors;
import com.dwinovo.chiikawa.testing.TestErrands;
import com.dwinovo.chiikawa.testing.TestView;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Playing the song on the music box: when the pet sets off on it, and what it holds the pet to. */
class PlayMusicOfferTest {
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
    void aSongThePetWouldPlayIsOneOfferThatGoesNowhere() {
        TestView view = TestView.at(HOME, FREE).withSong();

        assertEquals(1, TestErrands.offers(WorkData.PLAY_MUSIC, view).size());
        assertTrue(TestErrands.planOf(WorkData.PLAY_MUSIC, view).destination(view).isEmpty());
    }

    @Test
    void withoutOneThereIsNothingToPlayAndWhyIsSaid() {
        assertEquals("intent.chiikawa.fail.no_new_song",
            TestErrands.whyNoOffer(WorkData.PLAY_MUSIC, TestView.at(HOME, FREE)).reasonKey());
    }

    @Test
    void thePerformanceGoesOnUntilItsStepEndsItWhateverTheBoxSelectsNext() {
        TestView view = TestView.at(HOME, FREE).withSong();

        assertTrue(TestErrands.stillWanted(WorkData.PLAY_MUSIC, TestErrands.planOf(WorkData.PLAY_MUSIC, view),
            TestView.at(HOME, FREE)).ok());
    }

    /** What a labor board asks before it hands the pet a slip for music: a song to play, as the performance would need. */
    @Test
    void aPetIsReadyForTheMusicSlipOnlyWithASongToPlay() {
        WorkErrand<?, ?> errand = (WorkErrand<?, ?>) TestErrands.errand(WorkData.PLAY_MUSIC);

        assertTrue(errand.counts(WorkData.PLAY_MUSIC_SECOND));
        assertTrue(errand.readyFor(TestView.at(HOME, FREE).withSong()));
        assertFalse(errand.readyFor(TestView.at(HOME, FREE)));
    }

    /** Work with nothing special to need is always ready: the board's slip for it is nobody's loss. */
    @Test
    void workThatNeedsNothingInParticularIsAlwaysReady() {
        WorkErrand<?, ?> weeding = (WorkErrand<?, ?>) TestErrands.errand(WorkData.WEED);

        assertTrue(weeding.readyFor(TestView.at(HOME, FREE)));
    }
}
