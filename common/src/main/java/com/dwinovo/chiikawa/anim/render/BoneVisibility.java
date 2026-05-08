package com.dwinovo.chiikawa.anim.render;

import com.dwinovo.chiikawa.anim.baked.BakedModel;
import com.dwinovo.chiikawa.anim.state.PetAnimContext;

import java.util.List;
import java.util.Map;

/**
 * Pure-function helpers for evaluating {@link BoneVisibilityRule}s against
 * a model. Extracted out of {@link ChiikawaEntityRenderer} so the logic is
 * unit-testable without spinning up a Minecraft entity renderer.
 *
 * <h2>Semantics recap</h2>
 * Bones with no rule registered are <em>visible</em>. A bone with one or
 * more rules is visible when <em>any</em> rule votes {@code true} (OR
 * across rules). The output array is parallel to {@link BakedModel#bones}
 * and holds {@code true} for each bone that should be skipped during
 * rendering.
 */
public final class BoneVisibility {

    private static final boolean[] EMPTY = new boolean[0];

    private BoneVisibility() {}

    /**
     * Evaluate every registered rule against the given state, producing the
     * per-bone {@code hidden} flag array.
     *
     * @param rules registered rules keyed by bone name
     * @param model the entity's resolved baked model — needed to translate
     *              bone names to indices
     * @param state per-frame render-state snapshot (controllers populated)
     * @param ctx   gameplay-state snapshot consumed by rule lambdas
     * @return parallel-to-{@code model.bones} flag array; {@code true} =
     *         hidden. Returns an empty array when there are no rules or
     *         model is null (caller can substitute the empty sentinel
     *         without per-frame allocation).
     */
    public static boolean[] evaluate(Map<String, List<BoneVisibilityRule>> rules,
                                     BakedModel model,
                                     ChiikawaRenderState state,
                                     PetAnimContext ctx) {
        if (rules == null || rules.isEmpty() || model == null) return EMPTY;
        boolean[] hidden = new boolean[model.bones.length];
        for (Map.Entry<String, List<BoneVisibilityRule>> entry : rules.entrySet()) {
            Integer boneIdx = model.boneIndex.get(entry.getKey());
            if (boneIdx == null) continue; // rule for a bone the model lacks — silent skip
            boolean anyVisible = false;
            for (BoneVisibilityRule rule : entry.getValue()) {
                if (rule.isVisible(state, ctx)) {
                    anyVisible = true;
                    break;
                }
            }
            hidden[boneIdx] = !anyVisible;
        }
        return hidden;
    }
}
