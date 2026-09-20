package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.count;
import static com.dwinovo.chiikawa.gametest.GameTestKit.holding;
import static com.dwinovo.chiikawa.gametest.GameTestKit.worker;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.data.PetTaskTypeData;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitBlocks;
import com.dwinovo.chiikawa.init.InitRegistry;
import com.dwinovo.chiikawa.task.BoardSlips;
import com.dwinovo.chiikawa.task.BoardSlot;
import com.dwinovo.chiikawa.task.PetTaskTypes;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * The labor board, end to end: a pet that may not weed for free walks over, takes the
 * day's slip, does the work and comes away with emeralds.
 *
 * <p>This is the one case that has to hold for the whole of the mod's economy to mean
 * anything, and it is the one with the most between the first tick and the last — seeing,
 * reserving, walking, claiming, counting the work, being paid.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BoardGameTests {
    private static final String BATCH = "chiikawa_board";
    /** Everything above, at the speed a pet does it. */
    private static final int WORK_TICKS = 6000;

    private static final int STAND = 2;
    /** Grass to pull: more than the largest weeding slip asks for, so the work cannot run out. */
    private static final int WEED_PATCH = 6;

    @BeforeBatch(batch = BATCH)
    public static void settle(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    /** From a bare hoe to a handful of emeralds, with nothing said to the pet along the way. */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WORK_TICKS)
    public static void a_board_pays_a_farmer_for_pulling_weeds(GameTestHelper helper) {
        BlockPos board = weedingBoard(helper);
        helper.setBlock(board, InitBlocks.LABOR_BOARD.get());

        for (int x = 0; x < WEED_PATCH; x++) {
            for (int z = 0; z < WEED_PATCH; z++) {
                BlockPos weed = new BlockPos(2 + x, STAND, 2 + z);
                helper.setBlock(weed.below(), Blocks.GRASS_BLOCK);
                helper.setBlock(weed, Blocks.SHORT_GRASS);
            }
        }

        AbstractPet pet = holding(worker(helper, new BlockPos(4, STAND, 4)), Items.WOODEN_HOE);
        helper.succeedWhen(() -> helper.assertTrue(count(pet, Items.EMERALD) > 0,
            "the farmer never came away with anything for its trouble"));
    }

    /**
     * Where to put the board so that the first thing it offers a farmer today is weeding.
     *
     * <p>A board's day is rolled from the world seed, the day and its own position, so what
     * it offers is settled before anything is placed. Rather than reach into the block and
     * plant a slip — which would have the case testing a board nobody else could get — this
     * asks the same roll the board will ask, at each spot in turn, until it finds one that
     * offers the work this case is about.
     *
     * <p>The <i>first</i> farmer's slip, not any of them: picking mushrooms is a farmer's
     * work too, and a pet takes the first slip it can rather than the one a case had in
     * mind. A board that offers that first leaves the pet holding a slip it cannot finish
     * at noon, and the case waits out its clock for nothing.
     */
    private static BlockPos weedingBoard(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        long day = level.getDayTime() / Level.TICKS_PER_DAY;
        ResourceLocation farmer = InitRegistry.PET_JOB_REGISTRY.getKey(InitRegistry.FARMER.get());
        for (int x = 8; x < 15; x++) {
            for (int z = 2; z < 15; z++) {
                BlockPos rel = new BlockPos(x, STAND, z);
                long seed = BoardSlips.seed(level.getSeed(), day, helper.absolutePos(rel));
                List<BoardSlot> slips = BoardSlips.roll(seed, PetTaskTypes.all());
                if (slips.stream()
                    .filter(slot -> slot.slip().capability().equals(farmer))
                    .findFirst()
                    .filter(slot -> slot.slip().type().equals(PetTaskTypeData.WEEDING))
                    .isPresent()) {
                    return rel;
                }
            }
        }
        throw new AssertionError("nowhere on this floor offers a farmer weeding first today — "
            + "the roll, the weights or the slip types changed");
    }
}
