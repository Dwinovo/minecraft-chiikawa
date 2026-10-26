package com.dwinovo.chiikawa.anim.state;

import com.dwinovo.chiikawa.entity.PetDirective;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PetAnimationResolverTest {

    @Test
    void sitOverridesWhateverTheJobIsDoing() {
        PetAnimContext context = PetAnimContext.base(
                PetDirective.STAY, /*jobId=*/2, /*walkSpeed=*/0.0f, PetActivity.NONE);

        assertEquals(List.of("sit", "idle"), PetAnimationResolver.resolve(context));
    }

    /** A pet told to sit that runs, as it does from what hurts it, is not drawn sitting. */
    @Test
    void aSittingPetThatMovesMoves() {
        PetAnimContext context = PetAnimContext.base(
                PetDirective.STAY, /*jobId=*/2, /*walkSpeed=*/1.0f, PetActivity.NONE);

        assertEquals(List.of("walk", "run", "idle"), PetAnimationResolver.resolve(context));
    }

    @Test
    void idleWorkUsesJobSpecificLoopWithIdleFallback() {
        PetAnimContext context = PetAnimContext.base(
                PetDirective.FREE, /*jobId=*/1, /*walkSpeed=*/0.0f, PetActivity.NONE);

        assertEquals(List.of("work_idle_farmer", "idle"), PetAnimationResolver.resolve(context));
    }

    @Test
    void musicianWorkUsesMusicianLoopWithIdleFallback() {
        PetAnimContext context = PetAnimContext.base(
                PetDirective.FREE, /*jobId=*/4, /*walkSpeed=*/0.0f, PetActivity.NONE);

        assertEquals(List.of("work_idle_musician", "idle"), PetAnimationResolver.resolve(context));
    }

    @Test
    void activityOverridesModeAndMovement() {
        PetAnimContext context = PetAnimContext.base(
                PetDirective.FREE, /*jobId=*/1, /*walkSpeed=*/1.0f, PetActivity.PLAY_GUITAR);

        assertEquals(List.of("guitar", "idle"), PetAnimationResolver.resolve(context));
    }

    /** A pet with no exam animation of its own sits at the paper, as it sits anywhere. */
    @Test
    void anExamFallsBackToSitting() {
        PetAnimContext context = PetAnimContext.base(
                PetDirective.FOLLOW, /*jobId=*/0, /*walkSpeed=*/0.0f, PetActivity.EXAM);

        assertEquals(List.of("exam", "sit", "idle"), PetAnimationResolver.resolve(context));
    }

    @Test
    void aPartInASceneComesFirstAndFallsBackToWhatThePetWouldDoAnyway() {
        PetAnimContext context = PetAnimContext.base(
                PetDirective.FREE, /*jobId=*/1, /*walkSpeed=*/0.0f, PetActivity.NONE, "cling");

        assertEquals(List.of("cling", "work_idle_farmer", "idle"), PetAnimationResolver.resolve(context));
    }

    /** How a pet cowers is the first of several it has, and a sitting pet that cowers is drawn cowering. */
    @Test
    void severalAnimationsForAPerformanceAreTriedInOrderBeforeWhatThePetWouldDoAnyway() {
        PetAnimContext context = PetAnimContext.base(
                PetDirective.STAY, /*jobId=*/0, /*walkSpeed=*/0.0f, PetActivity.NONE, "cry,hurt");

        assertEquals(List.of("cry", "hurt", "sit", "idle"), PetAnimationResolver.resolve(context));
    }

    @Test
    void activityOverridesAPartInAScene() {
        PetAnimContext context = PetAnimContext.base(
                PetDirective.FREE, /*jobId=*/4, /*walkSpeed=*/0.0f, PetActivity.PLAY_GUITAR, "sit");

        assertEquals(List.of("guitar", "idle"), PetAnimationResolver.resolve(context));
    }

    @Test
    void semanticActionsKeepLegacyAnimationFallbacks() {
        assertEquals(PetAction.HARVEST, PetAction.fromNetworkId(PetAction.HARVEST.networkId()));
        assertEquals(PetAction.GENERIC_USE_MAINHAND, PetAction.fromLegacyAnimationName("use_mainhand"));
        assertTrue(PetAction.HARVEST.animationCandidates().contains("use_mainhand"));
        assertTrue(PetAction.BOW_DRAW.animationCandidates().contains("sword_attack"));
    }

    @Test
    void reactionsUseNetworkIdsAndNamedAnimationFallbacks() {
        assertEquals(PetReaction.CONFUSED, PetReaction.fromNetworkId(PetReaction.CONFUSED.networkId()));
        assertEquals(List.of("scratch_head", "confused"), PetReaction.CONFUSED.animationCandidates());
        assertTrue(PetReaction.REVIVE.animationCandidates().contains("happy"));
    }

    @Test
    void petActivityNetworkIdRoundTrip() {
        for (PetActivity a : PetActivity.values()) {
            assertEquals(a, PetActivity.fromNetworkId(a.networkId()));
        }
        assertEquals(PetActivity.NONE, PetActivity.fromNetworkId(255));
        assertEquals(PetActivity.NONE, PetActivity.fromNetworkId(-1));
    }

    @Test
    void guitarActivitySuppressesDailySoundsWithoutLegacySoundChannel() {
        assertFalse(PetActivity.PLAY_GUITAR.occupiesLongSoundChannel());
        assertTrue(PetActivity.PLAY_GUITAR.suppressesDailyPetSounds());
    }

    @Test
    void guitarActivityDurationIsOwnedByMusicSession() {
        assertEquals(0, PetActivity.PLAY_GUITAR.durationTicks());
    }

    @Test
    void noneActivityDoesNotSuppressDailySounds() {
        assertFalse(PetActivity.NONE.occupiesLongSoundChannel());
        assertFalse(PetActivity.NONE.suppressesDailyPetSounds());
        assertEquals(0, PetActivity.NONE.durationTicks());
    }
}
