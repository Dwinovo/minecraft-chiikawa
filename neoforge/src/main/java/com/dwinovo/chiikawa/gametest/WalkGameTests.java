package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.step.LiveHandle;
import com.dwinovo.chiikawa.init.InitEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.pathfinder.Path;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * How a pet is handed from one walk to the next: what an errand that ends leaves of its walk in the brain's mover is
 * not for the errand that follows it.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WalkGameTests {
    private static final String BATCH = "chiikawa_walk";
    private static final int STAND = 2;
    /** Long enough for the world to settle round a pet that has just been spawned. */
    private static final int SETTLED_TICKS = 3;

    @BeforeBatch(batch = BATCH)
    public static void settle(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    /**
     * Walking has stopped and a walk to a place a block from the last is set in the same tick, as when an errand ends
     * and the next sets off: the mover plans the new walk. It kept the path it had made for the last for as long as it
     * ran, and walked on along it, so that a pet strolling when it set off for a crop it could not reach walked on with
     * the stroll for up to a minute before it was told it had no way.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = 40)
    public static void a_walk_set_as_another_stops_is_planned_and_not_the_old_one_walked_on(GameTestHelper helper) {
        AbstractPet pet = helper.spawn(InitEntity.CHIIKAWA_PET.get(), new BlockPos(2, STAND, 2));
        // Its mind and its brain are ticked by hand, so that nothing else sets a walk or takes one away; without AI it does
        // not fall, and is put on the floor it was spawned on.
        pet.setNoAi(true);
        pet.setOnGround(true);
        LiveHandle handle = new LiveHandle(pet);
        ServerLevel level = helper.getLevel();
        BlockPos first = helper.absolutePos(new BlockPos(2, STAND, 10));
        BlockPos second = helper.absolutePos(new BlockPos(3, STAND, 10));

        helper.runAfterDelay(SETTLED_TICKS, () -> {
            handle.walkTo(first, 1.0F, 0);
            pet.getBrain().tick(level, pet);
            helper.assertTrue(planned(pet, first), "the mover did not plan the first walk: " + pet.getNavigation().getPath());
        });
        helper.runAfterDelay(SETTLED_TICKS + 1, () -> pet.getBrain().tick(level, pet));
        helper.runAfterDelay(SETTLED_TICKS + 2, () -> {
            handle.stopWalking();
            handle.walkTo(second, 1.0F, 0);
            pet.getBrain().tick(level, pet);
            helper.assertTrue(planned(pet, second), "the mover went on along the path to the last walk: " + pet.getNavigation().getPath());
            helper.succeed();
        });
    }

    private static boolean planned(AbstractPet pet, BlockPos target) {
        Path path = pet.getNavigation().getPath();
        return path != null && path.getTarget().equals(target);
    }
}
