package com.dwinovo.chiikawa.anim.state;

import com.dwinovo.chiikawa.entity.PetMode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PetAnimationResolverTest {

    @Test
    void sitOverridesMovement() {
        PetAnimContext context = PetAnimContext.base(
                PetMode.SIT, /*jobId=*/2, /*walkSpeed=*/1.0f, PetActivity.NONE);

        assertEquals(List.of("sit", "idle"), PetAnimationResolver.resolve(context));
    }

    @Test
    void idleWorkUsesJobSpecificLoopWithIdleFallback() {
        PetAnimContext context = PetAnimContext.base(
                PetMode.WORK, /*jobId=*/1, /*walkSpeed=*/0.0f, PetActivity.NONE);

        assertEquals(List.of("work_idle_farmer", "idle"), PetAnimationResolver.resolve(context));
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
}
