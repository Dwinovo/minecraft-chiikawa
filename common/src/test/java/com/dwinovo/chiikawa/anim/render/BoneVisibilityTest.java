package com.dwinovo.chiikawa.anim.render;

import com.dwinovo.chiikawa.anim.baked.BakedAnimation;
import com.dwinovo.chiikawa.anim.baked.BakedBone;
import com.dwinovo.chiikawa.anim.baked.BakedBoneChannel;
import com.dwinovo.chiikawa.anim.baked.BakedCube;
import com.dwinovo.chiikawa.anim.baked.BakedModel;
import com.dwinovo.chiikawa.anim.baked.LoopMode;
import com.dwinovo.chiikawa.anim.controller.BlendMode;
import com.dwinovo.chiikawa.anim.controller.ControllerSnapshot;
import com.dwinovo.chiikawa.anim.runtime.AnimationChannel;
import com.dwinovo.chiikawa.anim.state.PetAnimContext;
import com.dwinovo.chiikawa.entity.PetMode;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the bone visibility rule pipeline.
 *
 * <p>Covers:
 * <ul>
 *   <li>OR semantics across multiple rules on the same bone</li>
 *   <li>Bones without rules don't appear hidden in the output</li>
 *   <li>Rules referencing bones the model lacks are silently ignored</li>
 *   <li>Empty rules / null model short-circuits to the empty sentinel</li>
 *   <li>{@code isControllerPlaying} matches by both controller name AND animation name</li>
 * </ul>
 */
class BoneVisibilityTest {

    /** Build a tiny model with the given bone names (all roots, no cubes). */
    private static BakedModel modelOf(String... boneNames) {
        BakedBone[] bones = new BakedBone[boneNames.length];
        Map<String, Integer> idx = new HashMap<>();
        int[] roots = new int[boneNames.length];
        for (int i = 0; i < boneNames.length; i++) {
            bones[i] = new BakedBone(boneNames[i], -1,
                    0, 0, 0, 0, 0, 0, false, 0, 0, new int[0]);
            idx.put(boneNames[i], i);
            roots[i] = i;
        }
        return new BakedModel(bones, new BakedCube[0], roots, idx, 64, 64);
    }

    private static ChiikawaRenderState stateWithControllers(ControllerSnapshot... snapshots) {
        ChiikawaRenderState state = new ChiikawaRenderState();
        state.controllerSnapshots = snapshots;
        return state;
    }

    private static ControllerSnapshot snapshotOf(String controllerName, String animationName) {
        if (animationName == null) {
            return new ControllerSnapshot(controllerName, BlendMode.OVERRIDE,
                    null, null, 0L, 0f, false);
        }
        BakedAnimation anim = new BakedAnimation(animationName, 1f, LoopMode.LOOP, new BakedBoneChannel[0]);
        AnimationChannel channel = new AnimationChannel(anim, 0L, true);
        return new ControllerSnapshot(controllerName, BlendMode.OVERRIDE,
                channel, null, 0L, 0f, false);
    }

    private static PetAnimContext ctx() {
        return PetAnimContext.base(PetMode.FOLLOW, 0, 0f);
    }

    @Test
    void boneWithNoRuleStaysVisible() {
        BakedModel model = modelOf("body", "guitar");
        Map<String, List<BoneVisibilityRule>> rules = new HashMap<>();
        // Only register a rule for guitar — body has no rule.
        rules.put("guitar", List.of((s, c) -> false));
        boolean[] hidden = BoneVisibility.evaluate(rules, model, stateWithControllers(), ctx());

        assertEquals(2, hidden.length);
        assertFalse(hidden[0], "body has no rule → visible");
        assertTrue(hidden[1], "guitar's only rule voted hidden");
    }

    @Test
    void multipleRulesUseOrSemantics() {
        BakedModel model = modelOf("guitar");
        Map<String, List<BoneVisibilityRule>> rules = new HashMap<>();
        rules.put("guitar", List.of(
                (s, c) -> false,            // votes hidden
                (s, c) -> true,             // votes visible — should win
                (s, c) -> false));
        boolean[] hidden = BoneVisibility.evaluate(rules, model, stateWithControllers(), ctx());
        assertFalse(hidden[0], "any rule voting visible exposes the bone");
    }

    @Test
    void allRulesVotingHiddenHidesTheBone() {
        BakedModel model = modelOf("guitar");
        Map<String, List<BoneVisibilityRule>> rules = new HashMap<>();
        rules.put("guitar", List.of(
                (s, c) -> false,
                (s, c) -> false));
        boolean[] hidden = BoneVisibility.evaluate(rules, model, stateWithControllers(), ctx());
        assertTrue(hidden[0]);
    }

    @Test
    void ruleForUnknownBoneIsSilentlyIgnored() {
        BakedModel model = modelOf("body");
        Map<String, List<BoneVisibilityRule>> rules = new HashMap<>();
        // Rule references a bone the model doesn't have.
        rules.put("guitar_that_does_not_exist", List.of((s, c) -> false));
        boolean[] hidden = BoneVisibility.evaluate(rules, model, stateWithControllers(), ctx());
        // Body still visible (no rule applies); no exception thrown.
        assertEquals(1, hidden.length);
        assertFalse(hidden[0]);
    }

    @Test
    void emptyRulesReturnsEmptySentinel() {
        BakedModel model = modelOf("body");
        boolean[] hidden = BoneVisibility.evaluate(Map.of(), model, stateWithControllers(), ctx());
        assertEquals(0, hidden.length, "no rules → no per-frame allocation");
    }

    @Test
    void nullModelReturnsEmptySentinel() {
        Map<String, List<BoneVisibilityRule>> rules = new HashMap<>();
        rules.put("guitar", List.of((s, c) -> true));
        boolean[] hidden = BoneVisibility.evaluate(rules, null, stateWithControllers(), ctx());
        assertEquals(0, hidden.length);
    }

    @Test
    void isControllerPlayingMatchesByNameAndAnimation() {
        ChiikawaRenderState state = stateWithControllers(
                snapshotOf("main", "idle"),
                snapshotOf("action", "play_guitar"));

        assertTrue(ChiikawaEntityRenderer.isControllerPlaying(state, "action", "play_guitar"));
        assertFalse(ChiikawaEntityRenderer.isControllerPlaying(state, "action", "harvest"),
                "wrong animation name → no match");
        assertFalse(ChiikawaEntityRenderer.isControllerPlaying(state, "main", "play_guitar"),
                "wrong controller name → no match");
        assertFalse(ChiikawaEntityRenderer.isControllerPlaying(state, "missing", "play_guitar"),
                "controller not registered → no match");
    }

    @Test
    void isControllerPlayingHandlesIdleController() {
        ChiikawaRenderState state = stateWithControllers(
                snapshotOf("action", null));   // idle (no current animation)
        assertFalse(ChiikawaEntityRenderer.isControllerPlaying(state, "action", "play_guitar"),
                "idle controller never matches");
    }

    @Test
    void isAnyControllerPlayingMatchesIrrespectiveOfController() {
        ChiikawaRenderState state = stateWithControllers(
                snapshotOf("main", "idle"),
                snapshotOf("action", "guitar"));
        assertTrue(ChiikawaEntityRenderer.isAnyControllerPlaying(state, "guitar"),
                "any controller playing the named animation = true");
        assertFalse(ChiikawaEntityRenderer.isAnyControllerPlaying(state, "missing_anim"));

        // Even when the matching animation is on main rather than action.
        ChiikawaRenderState onMain = stateWithControllers(
                snapshotOf("main", "guitar"),
                snapshotOf("action", null));
        assertTrue(ChiikawaEntityRenderer.isAnyControllerPlaying(onMain, "guitar"),
                "controller-agnostic match works regardless of which controller plays it");
    }

    @Test
    void hachiwareGuitarRule_endToEnd() {
        // Concrete scenario: hachiware's guitar bone is hidden by default and
        // visible only when the action controller is sampling play_guitar.
        BakedModel model = modelOf("body", "RightHand", "guitar");
        Map<String, List<BoneVisibilityRule>> rules = new HashMap<>();
        rules.put("guitar", List.of(
                (s, c) -> ChiikawaEntityRenderer.isControllerPlaying(s, "action", "play_guitar")));

        // Case 1: action controller idle → guitar hidden.
        ChiikawaRenderState idleState = stateWithControllers(
                snapshotOf("main", "idle"),
                snapshotOf("action", null));
        boolean[] hidden = BoneVisibility.evaluate(rules, model, idleState, ctx());
        assertFalse(hidden[0], "body visible");
        assertFalse(hidden[1], "RightHand visible");
        assertTrue(hidden[2], "guitar hidden when not playing play_guitar");

        // Case 2: action controller plays play_guitar → guitar visible.
        ChiikawaRenderState playingState = stateWithControllers(
                snapshotOf("main", "idle"),
                snapshotOf("action", "play_guitar"));
        hidden = BoneVisibility.evaluate(rules, model, playingState, ctx());
        assertFalse(hidden[2], "guitar visible while play_guitar is sampled");
    }
}
