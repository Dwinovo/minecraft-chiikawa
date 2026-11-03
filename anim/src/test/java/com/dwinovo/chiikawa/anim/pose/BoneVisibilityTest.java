package com.dwinovo.chiikawa.anim.pose;

import com.dwinovo.chiikawa.anim.baked.BakedAnimation;
import com.dwinovo.chiikawa.anim.baked.BakedBone;
import com.dwinovo.chiikawa.anim.baked.BakedBoneChannel;
import com.dwinovo.chiikawa.anim.baked.BakedCube;
import com.dwinovo.chiikawa.anim.baked.BakedModel;
import com.dwinovo.chiikawa.anim.baked.LoopMode;
import com.dwinovo.chiikawa.anim.controller.BlendMode;
import com.dwinovo.chiikawa.anim.controller.ControllerSnapshot;
import com.dwinovo.chiikawa.anim.runtime.AnimationChannel;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
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
 *   <li>{@link PoseFrame#isPlaying} matches by both controller name AND animation name</li>
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

    private static PoseFrame stateWithControllers(ControllerSnapshot... snapshots) {
        PoseFrame state = new PoseFrame();
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

    @Test
    void boneWithNoRuleStaysVisible() {
        BakedModel model = modelOf("body", "guitar");
        BoneVisibility<PoseFrame> rules = new BoneVisibility<>();
        // Only register a rule for guitar — body has no rule.
        rules.add("guitar", s -> false);
        boolean[] hidden = rules.evaluate(model, stateWithControllers());

        assertEquals(2, hidden.length);
        assertFalse(hidden[0], "body has no rule → visible");
        assertTrue(hidden[1], "guitar's only rule voted hidden");
    }

    @Test
    void multipleRulesUseOrSemantics() {
        BakedModel model = modelOf("guitar");
        BoneVisibility<PoseFrame> rules = new BoneVisibility<>();
        rules.add("guitar", s -> false);            // votes hidden
        rules.add("guitar", s -> true);             // votes visible — should win
        rules.add("guitar", s -> false);
        boolean[] hidden = rules.evaluate(model, stateWithControllers());
        assertFalse(hidden[0], "any rule voting visible exposes the bone");
    }

    @Test
    void allRulesVotingHiddenHidesTheBone() {
        BakedModel model = modelOf("guitar");
        BoneVisibility<PoseFrame> rules = new BoneVisibility<>();
        rules.add("guitar", s -> false);
        rules.add("guitar", s -> false);
        boolean[] hidden = rules.evaluate(model, stateWithControllers());
        assertTrue(hidden[0]);
    }

    @Test
    void ruleForUnknownBoneIsSilentlyIgnored() {
        BakedModel model = modelOf("body");
        BoneVisibility<PoseFrame> rules = new BoneVisibility<>();
        // Rule references a bone the model doesn't have.
        rules.add("guitar_that_does_not_exist", s -> false);
        boolean[] hidden = rules.evaluate(model, stateWithControllers());
        // Body still visible (no rule applies); no exception thrown.
        assertEquals(1, hidden.length);
        assertFalse(hidden[0]);
    }

    @Test
    void emptyRulesReturnsEmptySentinel() {
        BakedModel model = modelOf("body");
        boolean[] hidden = new BoneVisibility<PoseFrame>().evaluate(model, stateWithControllers());
        assertEquals(0, hidden.length, "no rules → no per-frame allocation");
    }

    @Test
    void nullModelReturnsEmptySentinel() {
        BoneVisibility<PoseFrame> rules = new BoneVisibility<>();
        rules.add("guitar", s -> true);
        boolean[] hidden = rules.evaluate(null, stateWithControllers());
        assertEquals(0, hidden.length);
    }

    @Test
    void isControllerPlayingMatchesByNameAndAnimation() {
        PoseFrame state = stateWithControllers(
                snapshotOf("main", "idle"),
                snapshotOf("action", "play_guitar"));

        assertTrue(state.isPlaying( "action", "play_guitar"));
        assertFalse(state.isPlaying( "action", "harvest"),
                "wrong animation name → no match");
        assertFalse(state.isPlaying( "main", "play_guitar"),
                "wrong controller name → no match");
        assertFalse(state.isPlaying( "missing", "play_guitar"),
                "controller not registered → no match");
    }

    @Test
    void isControllerPlayingHandlesIdleController() {
        PoseFrame state = stateWithControllers(
                snapshotOf("action", null));   // idle (no current animation)
        assertFalse(state.isPlaying( "action", "play_guitar"),
                "idle controller never matches");
    }

    @Test
    void isAnyControllerPlayingMatchesIrrespectiveOfController() {
        PoseFrame state = stateWithControllers(
                snapshotOf("main", "idle"),
                snapshotOf("action", "guitar"));
        assertTrue(state.isAnyPlaying( "guitar"),
                "any controller playing the named animation = true");
        assertFalse(state.isAnyPlaying( "missing_anim"));

        // Even when the matching animation is on main rather than action.
        PoseFrame onMain = stateWithControllers(
                snapshotOf("main", "guitar"),
                snapshotOf("action", null));
        assertTrue(onMain.isAnyPlaying( "guitar"),
                "controller-agnostic match works regardless of which controller plays it");
    }

    @Test
    void hachiwareGuitarRule_endToEnd() {
        // Concrete scenario: hachiware's guitar bone is hidden by default and
        // visible only when the action controller is sampling play_guitar.
        BakedModel model = modelOf("body", "RightHand", "guitar");
        BoneVisibility<PoseFrame> rules = new BoneVisibility<>();
        rules.add("guitar", s -> s.isPlaying("action", "play_guitar"));

        // Case 1: action controller idle → guitar hidden.
        PoseFrame idleState = stateWithControllers(
                snapshotOf("main", "idle"),
                snapshotOf("action", null));
        boolean[] hidden = rules.evaluate(model, idleState);
        assertFalse(hidden[0], "body visible");
        assertFalse(hidden[1], "RightHand visible");
        assertTrue(hidden[2], "guitar hidden when not playing play_guitar");

        // Case 2: action controller plays play_guitar → guitar visible.
        PoseFrame playingState = stateWithControllers(
                snapshotOf("main", "idle"),
                snapshotOf("action", "play_guitar"));
        hidden = rules.evaluate(model, playingState);
        assertFalse(hidden[2], "guitar visible while play_guitar is sampled");
    }
}
