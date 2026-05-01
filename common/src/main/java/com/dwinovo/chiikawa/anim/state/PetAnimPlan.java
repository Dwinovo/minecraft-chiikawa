package com.dwinovo.chiikawa.anim.state;

import java.util.List;

/**
 * Channel-oriented animation plan produced by {@link PetAnimationResolver}.
 *
 * @param baseLoopCandidates ordered fallback list for layer 0
 * @param actionCandidates ordered fallback list for the action layer
 * @param overlayCandidates ordered fallback list for upper-body overlays
 * @param reactionCandidates ordered fallback list for reaction overlays
 */
public record PetAnimPlan(
        List<String> baseLoopCandidates,
        List<String> actionCandidates,
        List<String> overlayCandidates,
        List<String> reactionCandidates
) {
    public PetAnimPlan {
        baseLoopCandidates = immutableOrEmpty(baseLoopCandidates);
        actionCandidates = immutableOrEmpty(actionCandidates);
        overlayCandidates = immutableOrEmpty(overlayCandidates);
        reactionCandidates = immutableOrEmpty(reactionCandidates);
    }

    public static PetAnimPlan base(String... candidates) {
        return new PetAnimPlan(List.of(candidates), List.of(), List.of(), List.of());
    }

    private static List<String> immutableOrEmpty(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
