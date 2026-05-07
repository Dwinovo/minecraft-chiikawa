package com.dwinovo.chiikawa.anim.runtime;

import com.dwinovo.chiikawa.anim.baked.BakedAnimation;
import com.dwinovo.chiikawa.anim.baked.BakedBoneChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PetAnimatorStaleTest {

    private static BakedAnimation animation(String name, long bakeStamp) {
        return new BakedAnimation(name, 1.0f, true, new BakedBoneChannel[0], bakeStamp);
    }

    @Test
    void clearStaleDropsSlotsFromOlderBakeGenerations() {
        PetAnimator animator = new PetAnimator();
        BakedAnimation oldGen = animation("idle", 1L);
        BakedAnimation freshGen = animation("attack", 2L);

        animator.setMain(oldGen, true);
        animator.trigger(PetAnimator.Slot.ACTION, freshGen);
        animator.clearStale(2L);

        // BASE slot held a stamp-1 animation; it must be cleared.
        assertTrue(animator.get(PetAnimator.Slot.BASE).isEmpty());
        // ACTION slot held a stamp-2 animation; it must survive.
        assertSame(freshGen, animator.get(PetAnimator.Slot.ACTION).current().animation());
    }

    @Test
    void clearStaleIgnoresUnstampedAnimations() {
        PetAnimator animator = new PetAnimator();
        BakedAnimation legacy = new BakedAnimation("idle", 1.0f, true, new BakedBoneChannel[0]); // stamp 0

        animator.setMain(legacy, true);
        animator.clearStale(99L);

        // stamp 0 = "unset", treated as compatible with any current stamp.
        assertFalse(animator.get(PetAnimator.Slot.BASE).isEmpty());
    }

    @Test
    void clearStaleNoOpsWhenCurrentStampIsZero() {
        PetAnimator animator = new PetAnimator();
        BakedAnimation stampedAnim = animation("idle", 7L);

        animator.setMain(stampedAnim, true);
        animator.clearStale(0L);

        // No reload context — leave existing slots alone.
        assertFalse(animator.get(PetAnimator.Slot.BASE).isEmpty());
    }
}
