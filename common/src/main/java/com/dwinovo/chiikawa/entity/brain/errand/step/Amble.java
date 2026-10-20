package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import java.util.function.Function;
import net.minecraft.core.BlockPos;

/**
 * Walks without a hurry to a place, as far as the brain's own mover takes the pet: done when the mover lets go of the
 * walk, which it does on arriving, on finding no way, and at the end of a path that stops short, or when the time is
 * up. Neither is the pet giving up on anything; it simply stops ambling. Always interruptible.
 *
 * <p>Two ways to amble: {@link #stroll} to a spot a few blocks off, looking at it as it goes, and {@link #homeward}
 * to the middle of the area the pet keeps to.
 */
public final class Amble implements Step {
    private final Function<AbstractPet, BlockPos> spot;
    private final float speed;
    private final int maxTicks;
    private final boolean looking;
    private long startedAt;

    private Amble(Function<AbstractPet, BlockPos> spot, float speed, int maxTicks, boolean looking) {
        this.spot = spot;
        this.speed = speed;
        this.maxTicks = maxTicks;
        this.looking = looking;
    }

    /**
     * A stroll to a spot of the pet's choosing, within a few blocks: the first it tries that it can stand on, else
     * where it stands, which is no stroll.
     *
     * @param speed how fast
     * @param radius how far off the spot may be, each way
     * @param attempts how many spots it tries for one it can stand on
     * @param maxTicks how long the stroll lasts at the most
     */
    public static Amble stroll(float speed, int radius, int attempts, int maxTicks) {
        return new Amble(pet -> {
            BlockPos here = pet.blockPosition();
            for (int i = 0; i < attempts; i++) {
                BlockPos there = new BlockPos(here.getX() + pet.getRandom().nextInt(2 * radius + 1) - radius, here.getY(),
                    here.getZ() + pet.getRandom().nextInt(2 * radius + 1) - radius);
                if (pet.getNavigation().isStableDestination(there)) {
                    return there;
                }
            }
            return here;
        }, speed, maxTicks, true);
    }

    /**
     * A walk back to a place, without looking at it.
     *
     * @param center where to
     * @param speed how fast
     * @param maxTicks how long the walk lasts at the most
     */
    public static Amble homeward(BlockPos center, float speed, int maxTicks) {
        return new Amble(pet -> center, speed, maxTicks, false);
    }

    @Override
    public void begin(Handle handle) {
        startedAt = handle.now();
        BlockPos there = spot.apply(LiveHandle.of(handle).pet());
        handle.walkTo(there, speed, 0);
        if (looking) {
            handle.lookAt(there);
        }
    }

    @Override
    public StepStatus tick(Handle handle) {
        return !handle.walking() || handle.now() - startedAt >= maxTicks ? StepStatus.DONE : StepStatus.RUNNING;
    }
}
