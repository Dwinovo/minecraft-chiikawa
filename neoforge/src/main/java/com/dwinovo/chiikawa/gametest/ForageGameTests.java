package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.MIDNIGHT;
import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.holding;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;
import static com.dwinovo.chiikawa.gametest.GameTestKit.wildWorker;
import static com.dwinovo.chiikawa.gametest.GameTestKit.worker;

import com.dwinovo.chiikawa.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * Weeding and mushrooms — the work a pet may do off its own back, and the rule that says
 * when it may not.
 *
 * <p>A pet with an owner does this work for a slip and not otherwise; a wild one does it
 * because nobody is paying it either way. That rule is the whole reason the labor board
 * exists, so it gets a case from both sides.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ForageGameTests {
    private static final String DAY_BATCH = "chiikawa_forage_day";
    private static final String NIGHT_BATCH = "chiikawa_forage_night";
    private static final int WORK_TICKS = 3600;
    /** How long a pet is watched leaving something alone before we believe it means to. */
    private static final int LEAVE_IT_TICKS = 600;

    private static final int STAND = 2;

    @BeforeBatch(batch = DAY_BATCH)
    public static void settleDay(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    @BeforeBatch(batch = NIGHT_BATCH)
    public static void settleNight(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, MIDNIGHT);
    }

    /** Nobody asked, nobody is paying: a wild farmer tidies the grass because it is there. */
    @GameTest(template = "floor16", batch = DAY_BATCH, timeoutTicks = WORK_TICKS)
    public static void a_wild_farmer_pulls_weeds_unasked(GameTestHelper helper) {
        holding(wildWorker(helper, new BlockPos(4, STAND, 4)), Items.WOODEN_HOE);
        BlockPos weed = new BlockPos(7, STAND, 4);
        helper.setBlock(weed.below(), Blocks.GRASS_BLOCK);
        helper.setBlock(weed, Blocks.SHORT_GRASS);

        helper.succeedWhen(() -> helper.assertBlockPresent(Blocks.AIR, weed));
    }

    /**
     * The same grass, the same hoe, but the pet has an owner and no slip. It leaves it
     * standing: work an owner benefits from is work somebody should be paying for.
     */
    @GameTest(template = "floor16", batch = DAY_BATCH, timeoutTicks = LEAVE_IT_TICKS + 100)
    public static void an_owned_farmer_waits_for_a_slip(GameTestHelper helper) {
        holding(worker(helper, new BlockPos(4, STAND, 4)), Items.WOODEN_HOE);
        BlockPos weed = new BlockPos(7, STAND, 4);
        helper.setBlock(weed.below(), Blocks.GRASS_BLOCK);
        helper.setBlock(weed, Blocks.SHORT_GRASS);

        helper.runAtTickTime(LEAVE_IT_TICKS, () -> {
            helper.assertBlockPresent(Blocks.SHORT_GRASS, weed);
            helper.succeed();
        });
    }

    /** Mushrooms are picked after dark, which is when they are worth picking. */
    @GameTest(template = "floor16", batch = NIGHT_BATCH, timeoutTicks = WORK_TICKS)
    public static void a_wild_farmer_picks_mushrooms_at_night(GameTestHelper helper) {
        holding(wildWorker(helper, new BlockPos(4, STAND, 4)), Items.WOODEN_HOE);
        BlockPos mushroom = new BlockPos(7, STAND, 4);
        helper.setBlock(mushroom, Blocks.RED_MUSHROOM);

        helper.succeedWhen(() -> helper.assertBlockPresent(Blocks.AIR, mushroom));
    }

    /** By day it walks past them: the picking is a night's errand, not a standing order. */
    @GameTest(template = "floor16", batch = DAY_BATCH, timeoutTicks = LEAVE_IT_TICKS + 100)
    public static void a_wild_farmer_leaves_mushrooms_by_day(GameTestHelper helper) {
        holding(wildWorker(helper, new BlockPos(4, STAND, 4)), Items.WOODEN_HOE);
        BlockPos mushroom = new BlockPos(7, STAND, 4);
        helper.setBlock(mushroom, Blocks.RED_MUSHROOM);

        helper.runAtTickTime(LEAVE_IT_TICKS, () -> {
            helper.assertBlockPresent(Blocks.RED_MUSHROOM, mushroom);
            helper.succeed();
        });
    }
}
