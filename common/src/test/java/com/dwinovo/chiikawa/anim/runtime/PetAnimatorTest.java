package com.dwinovo.chiikawa.anim.runtime;

import com.dwinovo.chiikawa.anim.baked.BakedAnimation;
import com.dwinovo.chiikawa.anim.baked.BakedBoneChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PetAnimatorTest {

    @Test
    void oneShotFinishesAtBakedDuration() {
        BakedAnimation animation = new BakedAnimation("one_shot", 0.5f, false, new BakedBoneChannel[0]);
        AnimationChannel channel = new AnimationChannel(animation, 1_000_000_000L, false);

        assertFalse(PetAnimator.isFinished(channel, 1_499_999_999L));
        assertTrue(PetAnimator.isFinished(channel, 1_500_000_000L));
    }

    @Test
    void loopingChannelsDoNotFinishAutomatically() {
        BakedAnimation animation = new BakedAnimation("loop", 0.5f, true, new BakedBoneChannel[0]);
        AnimationChannel channel = new AnimationChannel(animation, 1_000_000_000L, true);

        assertFalse(PetAnimator.isFinished(channel, 10_000_000_000L));
    }

    @Test
    void clearFinishedKeepsBaseAndClearsUpperOneShots() {
        BakedAnimation base = new BakedAnimation("idle", 1.0f, true, new BakedBoneChannel[0]);
        BakedAnimation instant = new BakedAnimation("instant", 0.0f, false, new BakedBoneChannel[0]);
        PetAnimator animator = new PetAnimator();

        animator.setMain(base, true);
        animator.trigger(PetAnimator.Slot.ACTION, instant);
        animator.trigger(PetAnimator.Slot.REACTION, instant);
        animator.clearFinished(System.nanoTime());

        assertSame(base, animator.get(PetAnimator.Slot.BASE).current().animation());
        assertTrue(animator.get(PetAnimator.Slot.ACTION).isEmpty());
        assertTrue(animator.get(PetAnimator.Slot.REACTION).isEmpty());
    }

    @Test
    void firstMainAnimationDoesNotCreateFade() {
        BakedAnimation idle = new BakedAnimation("idle", 1.0f, true, new BakedBoneChannel[0]);
        PetAnimator animator = new PetAnimator();

        animator.setMain(idle, true);

        SlotState slot = animator.get(PetAnimator.Slot.BASE);
        assertSame(idle, slot.current().animation());
        assertNull(slot.previous());
        assertFalse(slot.hasFade());
    }

    @Test
    void switchingMainAnimationCreatesFade() {
        BakedAnimation idle = new BakedAnimation("idle", 1.0f, true, new BakedBoneChannel[0]);
        BakedAnimation run = new BakedAnimation("run", 1.0f, true, new BakedBoneChannel[0]);
        PetAnimator animator = new PetAnimator();

        animator.setMain(idle, true);
        animator.setMain(run, true, 0.25f);

        SlotState slot = animator.get(PetAnimator.Slot.BASE);
        assertSame(run, slot.current().animation());
        assertSame(idle, slot.previous().animation());
        assertTrue(slot.hasFade());
    }

    @Test
    void fadeFinishesAtConfiguredDuration() {
        BakedAnimation idle = new BakedAnimation("idle", 1.0f, true, new BakedBoneChannel[0]);
        BakedAnimation run = new BakedAnimation("run", 1.0f, true, new BakedBoneChannel[0]);
        long startNs = 1_000_000_000L;
        SlotState slot = SlotState.withFade(
                new AnimationChannel(run, startNs, true),
                new AnimationChannel(idle, startNs, true),
                startNs, 0.1f);

        assertFalse(PetAnimator.isFadeFinished(slot, 1_099_999_999L));
        assertTrue(PetAnimator.isFadeFinished(slot, 1_100_000_000L));
    }
}
