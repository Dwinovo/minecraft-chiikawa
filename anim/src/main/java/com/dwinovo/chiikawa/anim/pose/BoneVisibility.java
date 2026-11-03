package com.dwinovo.chiikawa.anim.pose;

import com.dwinovo.chiikawa.anim.baked.BakedModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The {@link BoneVisibilityRule}s of one kind of entity, and their evaluation against a
 * model: a plain object, so the logic is unit-testable without a game around it.
 *
 * <h2>Semantics recap</h2>
 * Bones with no rule registered are <em>visible</em>. A bone with one or
 * more rules is visible when <em>any</em> rule votes {@code true} (OR
 * across rules). The output array is parallel to {@link BakedModel#bones}
 * and holds {@code true} for each bone that should be skipped during
 * rendering.
 *
 * @param <S> the caller's per-frame state the rules read; the engine never looks inside it
 */
public final class BoneVisibility<S> {

    private static final boolean[] EMPTY = new boolean[0];

    /** Rules keyed by bone name. */
    private final Map<String, List<BoneVisibilityRule<S>>> rules = new HashMap<>();

    /**
     * Registers a per-frame visibility predicate for a named bone. The bone (and its entire
     * subtree) is hidden when no registered rule for that bone votes visible. Registering
     * several rules for the same bone composes with OR semantics (any vote shows the bone).
     */
    public void add(String boneName, BoneVisibilityRule<S> rule) {
        rules.computeIfAbsent(boneName, k -> new ArrayList<>()).add(rule);
    }

    /**
     * Evaluate every registered rule against the given state, producing the
     * per-bone {@code hidden} flag array.
     *
     * @param model the entity's resolved baked model — needed to translate
     *              bone names to indices
     * @param state per-frame state consumed by rule lambdas
     * @return parallel-to-{@code model.bones} flag array; {@code true} =
     *         hidden. Returns an empty array when there are no rules or
     *         model is null (caller can substitute the empty sentinel
     *         without per-frame allocation).
     */
    public boolean[] evaluate(BakedModel model, S state) {
        if (rules.isEmpty() || model == null) return EMPTY;
        boolean[] hidden = new boolean[model.bones.length];
        for (Map.Entry<String, List<BoneVisibilityRule<S>>> entry : rules.entrySet()) {
            Integer boneIdx = model.boneIndex.get(entry.getKey());
            if (boneIdx == null) continue; // rule for a bone the model lacks — silent skip
            boolean anyVisible = false;
            for (BoneVisibilityRule<S> rule : entry.getValue()) {
                if (rule.isVisible(state)) {
                    anyVisible = true;
                    break;
                }
            }
            hidden[boneIdx] = !anyVisible;
        }
        return hidden;
    }
}
