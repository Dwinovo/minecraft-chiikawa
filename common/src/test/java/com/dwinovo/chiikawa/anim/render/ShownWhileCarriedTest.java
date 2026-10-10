package com.dwinovo.chiikawa.anim.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.anim.baked.BakedAnimation;
import com.dwinovo.chiikawa.anim.baked.BakedBoneChannel;
import com.dwinovo.chiikawa.anim.baked.LoopMode;
import com.dwinovo.chiikawa.anim.controller.BlendMode;
import com.dwinovo.chiikawa.anim.controller.ControllerSnapshot;
import com.dwinovo.chiikawa.anim.runtime.AnimationChannel;
import com.dwinovo.chiikawa.anim.state.PetAnimContext;
import com.dwinovo.chiikawa.entity.PetDirective;
import java.util.List;
import org.junit.jupiter.api.Test;

/** A camera hangs at the chest while it is carried, and is lifted, or put away, by what the pet is doing. */
class ShownWhileCarriedTest {
    private static final ShownWhileCarried RULE =
        new ShownWhileCarried(PetData.CARRIES_CAMERA, PropBone.animationsClashingWithCamera());

    private static ChiikawaRenderState state(boolean carried, String... playing) {
        ChiikawaRenderState state = new ChiikawaRenderState();
        state.put(PetData.CARRIES_CAMERA, carried);
        ControllerSnapshot[] snapshots = new ControllerSnapshot[playing.length];
        for (int i = 0; i < playing.length; i++) {
            BakedAnimation animation = new BakedAnimation(playing[i], 1f, LoopMode.LOOP, new BakedBoneChannel[0]);
            snapshots[i] = new ControllerSnapshot("main", BlendMode.OVERRIDE,
                new AnimationChannel(animation, 0L, true), null, 0L, 0f, false);
        }
        state.controllerSnapshots = snapshots;
        return state;
    }

    private static boolean shown(ChiikawaRenderState state) {
        PetAnimContext ctx = PetAnimContext.base(PetDirective.FOLLOW, 0, 0f);
        return RULE.isVisible(state, ctx);
    }

    /** What the renderer does to the camera bone: any of its rules, the lift or the hanging, shows it. */
    private static boolean drawn(ChiikawaRenderState state) {
        PetAnimContext ctx = PetAnimContext.base(PetDirective.FOLLOW, 0, 0f);
        return PropBone.CAMERA.shownDuring().isVisible(state, ctx) || RULE.isVisible(state, ctx);
    }

    @Test
    void aCameraHangsAtTheChestWhileCarried() {
        assertTrue(shown(state(true)));
        assertTrue(shown(state(true, "idle")));
        assertTrue(shown(state(true, "walk")));
    }

    @Test
    void withoutACameraThereIsNothingAtTheChest() {
        assertFalse(shown(state(false)));
        assertFalse(shown(state(false, "idle")));
        assertFalse(drawn(state(false, "idle")));
    }

    @Test
    void anUnsetFlagIsNoCamera() {
        assertFalse(shown(new ChiikawaRenderState()));
    }

    @Test
    void theGuitarAndTheExamPutItAway() {
        for (String animation : List.of("guitar", "exam", "hand_in", "read_results")) {
            assertFalse(shown(state(true, animation)), animation + " leaves the camera on the chest");
            assertFalse(drawn(state(true, animation)), animation + " leaves the camera on the chest");
        }
    }

    @Test
    void thePhotoLiftsItWhateverElseIsGoingOn() {
        assertTrue(drawn(state(true, "photo")));
    }

    @Test
    void whatPutsItAwayIsWhatShowsTheOtherProps() {
        List<String> clashing = PropBone.animationsClashingWithCamera();

        assertTrue(clashing.containsAll(List.of("guitar", "exam", "hand_in")), "a prop's animation was left out: " + clashing);
        assertFalse(clashing.contains("photo"), "the lift puts the camera away");
        assertEquals(clashing.size(), clashing.stream().distinct().count());
    }
}
