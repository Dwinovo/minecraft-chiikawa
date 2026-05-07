package com.dwinovo.chiikawa.anim.runtime;

import com.dwinovo.chiikawa.anim.baked.BakedAnimation;
import com.dwinovo.chiikawa.anim.baked.BakedBoneChannel;
import com.dwinovo.chiikawa.anim.baked.LoopMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * isFinished must respect the underlying animation's {@link LoopMode} —
 * specifically, HOLD_ON_LAST_FRAME animations never report finished even
 * after their duration has elapsed, so the slot retains the last-frame pose
 * until something else evicts it.
 */
class PetAnimatorHoldTest {

    @Test
    void playOnceFinishesAtDuration() {
        BakedAnimation playOnce = new BakedAnimation("attack", 0.5f,
                LoopMode.PLAY_ONCE, new BakedBoneChannel[0]);
        AnimationChannel ch = new AnimationChannel(playOnce, 1_000_000_000L, false);

        assertFalse(PetAnimator.isFinished(ch, 1_499_999_999L));
        assertTrue(PetAnimator.isFinished(ch, 1_500_000_000L));
    }

    @Test
    void holdOnLastFrameNeverFinishes() {
        BakedAnimation hold = new BakedAnimation("death", 0.5f,
                LoopMode.HOLD_ON_LAST_FRAME, new BakedBoneChannel[0]);
        AnimationChannel ch = new AnimationChannel(hold, 1_000_000_000L, false);

        // Way past the duration — should still report unfinished so the slot
        // keeps the last-frame pose.
        assertFalse(PetAnimator.isFinished(ch, 999_000_000_000L));
    }

    @Test
    void clearFinishedKeepsHoldSlotsAlive() {
        BakedAnimation hold = new BakedAnimation("death", 0.5f,
                LoopMode.HOLD_ON_LAST_FRAME, new BakedBoneChannel[0]);
        BakedAnimation playOnce = new BakedAnimation("instant", 0.0f,
                LoopMode.PLAY_ONCE, new BakedBoneChannel[0]);

        PetAnimator animator = new PetAnimator();
        animator.trigger(PetAnimator.Slot.ACTION, hold);
        animator.trigger(PetAnimator.Slot.REACTION, playOnce);
        animator.clearFinished(System.nanoTime());

        // Hold slot survives the sweep, play-once slot is gone.
        assertSame(hold, animator.get(PetAnimator.Slot.ACTION).current().animation());
        assertTrue(animator.get(PetAnimator.Slot.REACTION).isEmpty());
    }
}
