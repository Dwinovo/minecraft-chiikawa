package com.dwinovo.chiikawa.anim.state;

import com.dwinovo.chiikawa.entity.PetMode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PetAnimationResolverTest {

    @Test
    void sitOverridesMovement() {
        PetAnimContext context = new PetAnimContext(
                PetMode.SIT,
                PetJobRole.FENCER,
                PetLocomotion.RUN,
                PetAction.NONE,
                PetReaction.NONE,
                PetAttention.NONE);

        PetAnimPlan plan = PetAnimationResolver.resolve(context);

        assertEquals(List.of("sit", "idle"), plan.baseLoopCandidates());
    }

    @Test
    void idleWorkUsesJobSpecificLoopWithIdleFallback() {
        PetAnimContext context = new PetAnimContext(
                PetMode.WORK,
                PetJobRole.FARMER,
                PetLocomotion.IDLE,
                PetAction.NONE,
                PetReaction.NONE,
                PetAttention.NONE);

        PetAnimPlan plan = PetAnimationResolver.resolve(context);

        assertEquals(List.of("work_idle_farmer", "idle"), plan.baseLoopCandidates());
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
}
