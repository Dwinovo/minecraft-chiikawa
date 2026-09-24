package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.holding;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;
import static com.dwinovo.chiikawa.gametest.GameTestKit.wildWorker;
import static com.dwinovo.chiikawa.gametest.GameTestKit.worker;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.constraint.AnchorDistances;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;

/**
 * How far a pet may go, and what it may do when it gets there.
 *
 * <p>A free-roaming pet keeps to the patch it was left on. Without that an owner who says
 * "stay around here and get on with it" has said nothing at all, and the pet is gone by
 * morning.
 */
@GameTestHolder(Constants.MOD_ID)
public final class AnchorGameTests {
    private static final String BATCH = "chiikawa_anchor";
    private static final int WALK_HOME_TICKS = 3600;
    private static final int LEAVE_IT_TICKS = 600;

    private static final int STAND = 2;
    /** Where a pet is left, and the middle of the patch it should keep to. */
    private static final BlockPos HOME = new BlockPos(4, STAND, 4);
    /** Far enough out to be past its leash, which is what should send it back. */
    private static final BlockPos STRAY = new BlockPos(28, STAND, 28);

    @BeforeBatch(batch = BATCH)
    public static void settle(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    /** Carried off to the far corner, it walks itself home. */
    @GameTest(template = "floor32", batch = BATCH, timeoutTicks = WALK_HOME_TICKS)
    public static void a_free_pet_comes_back_when_it_strays_too_far(GameTestHelper helper) {
        AbstractPet pet = holding(worker(helper, HOME), Items.WOODEN_HOE);
        BlockPos home = helper.absolutePos(HOME);
        BlockPos stray = helper.absolutePos(STRAY);
        pet.teleportTo(stray.getX() + 0.5, stray.getY(), stray.getZ() + 0.5);

        helper.succeedWhen(() -> helper.assertTrue(
            pet.blockPosition().distSqr(home) < AnchorDistances.FREE_REACH * AnchorDistances.FREE_REACH,
            Component.literal("the pet stayed out at the far corner instead of going home")));
    }

    /**
     * A wild pet leaves a crop standing. Harvesting is work an owner set up and expects to
     * find done; a wild one that helped itself to a field would be a pest, not a neighbour.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = LEAVE_IT_TICKS + 100)
    public static void a_wild_pet_leaves_a_crop_standing(GameTestHelper helper) {
        holding(wildWorker(helper, new BlockPos(4, STAND, 4)), Items.WOODEN_HOE);
        BlockPos crop = new BlockPos(7, STAND, 4);
        helper.setBlock(crop.below(), Blocks.FARMLAND);
        helper.setBlock(crop, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, CropBlock.MAX_AGE));

        helper.runAtTickTime(LEAVE_IT_TICKS, () -> {
            helper.assertBlockProperty(crop, CropBlock.AGE, CropBlock.MAX_AGE);
            helper.succeed();
        });
    }
}
