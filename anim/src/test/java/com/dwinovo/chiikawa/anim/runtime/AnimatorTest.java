package com.dwinovo.chiikawa.anim.runtime;

import com.dwinovo.chiikawa.anim.baked.BakedAnimation;
import com.dwinovo.chiikawa.anim.baked.BakedBoneChannel;
import com.dwinovo.chiikawa.anim.baked.LoopMode;
import com.dwinovo.chiikawa.anim.controller.BlendMode;
import com.dwinovo.chiikawa.anim.controller.ControllerConfig;
import com.dwinovo.chiikawa.anim.controller.ControllerHandler;
import com.dwinovo.chiikawa.anim.controller.ControllerRoster;
import com.dwinovo.chiikawa.anim.controller.ControllerSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The animator is advanced once a frame with the roster, and hands back what each controller plays. */
class AnimatorTest {

    private static BakedAnimation loop(String name, long bakeStamp) {
        return new BakedAnimation(name, 1f, LoopMode.LOOP, new BakedBoneChannel[0], bakeStamp);
    }

    private static BakedAnimation once(String name) {
        return new BakedAnimation(name, 1f, LoopMode.PLAY_ONCE, new BakedBoneChannel[0]);
    }

    /** A "main" controller that plays whatever the input is, and an "action" one that only runs on triggers. */
    private static ControllerRoster<BakedAnimation> roster() {
        ControllerRoster<BakedAnimation> roster = new ControllerRoster<>();
        roster.add(new ControllerConfig<>("main", BlendMode.OVERRIDE, 0f, input -> input));
        roster.add(new ControllerConfig<>("action", BlendMode.OVERRIDE, 0f, ControllerHandler::neverPlay));
        return roster;
    }

    @Test
    void everyFrameGivesOneSnapshotPerControllerInRegistrationOrder() {
        Animator<BakedAnimation> animator = new Animator<>();
        BakedAnimation idle = loop("idle", 0L);

        ControllerSnapshot[] snapshots = animator.advance(roster(), idle, 7L, 1_000_000_000L, 0L);

        assertEquals(2, snapshots.length);
        assertEquals("main", snapshots[0].name());
        assertSame(idle, snapshots[0].current().animation());
        assertEquals("action", snapshots[1].name());
        assertNull(snapshots[1].current());
    }

    @Test
    void aTriggerLandsOnTheControllerItNames() {
        Animator<BakedAnimation> animator = new Animator<>();
        ControllerRoster<BakedAnimation> roster = roster();
        BakedAnimation harvest = once("harvest");
        animator.advance(roster, loop("idle", 0L), 7L, 0L, 0L);

        animator.playOnce("action", harvest, 0L);
        animator.playOnce("missing", once("never"), 0L);
        ControllerSnapshot[] snapshots = animator.advance(roster, loop("idle", 0L), 7L, 100_000_000L, 0L);

        assertSame(harvest, snapshots[1].current().animation());
    }

    @Test
    void anAnimationFromBeforeAReloadIsDroppedOnceTheModelHasANewStamp() {
        Animator<BakedAnimation> animator = new Animator<>();
        ControllerRoster<BakedAnimation> roster = new ControllerRoster<>();
        roster.add(new ControllerConfig<>("main", BlendMode.OVERRIDE, 0f, input -> null));
        BakedAnimation stale = loop("idle", 1L);
        animator.advance(roster, null, 7L, 0L, 1L);
        animator.playOnce("main", stale, 0L);
        assertSame(stale, animator.advance(roster, null, 7L, 1L, 1L)[0].current().animation());

        ControllerSnapshot[] after = animator.advance(roster, null, 7L, 2L, 2L);

        assertNull(after[0].current(), "the controller kept an animation baked before the reload");
    }

    @Test
    void twoControllersMayNotShareAName() {
        ControllerRoster<BakedAnimation> roster = roster();
        assertThrows(IllegalStateException.class,
            () -> roster.add(new ControllerConfig<>("main", BlendMode.ADDITIVE, 0f, ControllerHandler::neverPlay)));
    }

    @Test
    void entitiesMadeAtTheSameInstantLoopOutOfStep() {
        ControllerRoster<BakedAnimation> roster = roster();
        BakedAnimation idle = loop("idle", 0L);
        Animator<BakedAnimation> a = new Animator<>();
        Animator<BakedAnimation> b = new Animator<>();

        long startA = a.advance(roster, idle, 1L, 5_000_000_000L, 0L)[0].current().startTimeNs();
        long startB = b.advance(roster, idle, 4000L, 5_000_000_000L, 0L)[0].current().startTimeNs();

        assertEquals(3_999_000_000L, startA - startB);
    }
}
