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

        assertSame(base, animator.get(PetAnimator.Slot.BASE).animation());
        assertNull(animator.get(PetAnimator.Slot.ACTION));
        assertNull(animator.get(PetAnimator.Slot.REACTION));
    }

    @Test
    void firstMainAnimationDoesNotCreateTransition() {
        BakedAnimation idle = new BakedAnimation("idle", 1.0f, true, new BakedBoneChannel[0]);
        PetAnimator animator = new PetAnimator();

        animator.setMain(idle, true);

        assertSame(idle, animator.get(PetAnimator.Slot.BASE).animation());
        assertNull(animator.get(PetAnimator.Slot.BASE).transition());
    }

    @Test
    void switchingMainAnimationCreatesGenericTransition() {
        BakedAnimation idle = new BakedAnimation("idle", 1.0f, true, new BakedBoneChannel[0]);
        BakedAnimation run = new BakedAnimation("run", 1.0f, true, new BakedBoneChannel[0]);
        PetAnimator animator = new PetAnimator();

        animator.setMain(idle, true);
        animator.setMain(run, true, 0.25f);

        AnimationChannel channel = animator.get(PetAnimator.Slot.BASE);
        assertSame(run, channel.animation());
        assertSame(idle, channel.transition().fromChannel().animation());
        assertSame(channel.transition().fromChannel(), channel.transition().fromChannel().withoutTransition());
    }

    @Test
    void transitionFinishesAtConfiguredDuration() {
        BakedAnimation idle = new BakedAnimation("idle", 1.0f, true, new BakedBoneChannel[0]);
        long startNs = 1_000_000_000L;
        AnimationChannel from = new AnimationChannel(idle, startNs, true);
        AnimationTransition transition = new AnimationTransition(from, startNs, 0.1f);

        assertFalse(PetAnimator.isTransitionFinished(transition, 1_099_999_999L));
        assertTrue(PetAnimator.isTransitionFinished(transition, 1_100_000_000L));
    }
}
