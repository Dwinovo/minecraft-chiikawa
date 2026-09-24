package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.MIDNIGHT;
import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.holding;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;
import static com.dwinovo.chiikawa.gametest.GameTestKit.wildWorker;
import static com.dwinovo.chiikawa.gametest.GameTestKit.worker;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.data.PetTaskTypeData;
import com.dwinovo.chiikawa.init.InitBlocks;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.init.InitRegistry;
import com.dwinovo.chiikawa.item.PetDollData;
import com.dwinovo.chiikawa.task.PetTask;
import com.dwinovo.chiikawa.task.PetWorkCounters;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * The rules around a slip that are easy to state and easy to break: who may take one and
 * when, and what happens to one a pet was holding when it died.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SlipGameTests {
    private static final String DAY_BATCH = "chiikawa_slip_day";
    private static final String NIGHT_BATCH = "chiikawa_slip_night";
    private static final int LEAVE_IT_TICKS = 600;
    private static final int DEATH_TICKS = 200;

    private static final int STAND = 2;

    @BeforeBatch(batch = DAY_BATCH)
    public static void settleDay(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    @BeforeBatch(batch = NIGHT_BATCH)
    public static void settleNight(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, MIDNIGHT);
    }

    /**
     * A wild pet waits half the day before touching the board. The slips are put up for the
     * pets somebody keeps; a wild one takes what is left over in the afternoon, and at noon
     * there is no such thing as left over.
     */
    @GameTest(template = "floor16", batch = DAY_BATCH, timeoutTicks = LEAVE_IT_TICKS + 100)
    public static void a_wild_pet_leaves_the_morning_slips_alone(GameTestHelper helper) {
        AbstractPet pet = holding(wildWorker(helper, new BlockPos(4, STAND, 4)), Items.WOODEN_HOE);
        helper.setBlock(new BlockPos(7, STAND, 4), InitBlocks.LABOR_BOARD.get());

        helper.runAtTickTime(LEAVE_IT_TICKS, () -> {
            helper.assertTrue(pet.getTask().isEmpty(), "a wild pet took a slip before the afternoon");
            helper.succeed();
        });
    }

    /**
     * Mushrooms at night are still work, and an owned pet still needs a slip for work.
     * Night is what makes picking possible, not what makes it free.
     */
    @GameTest(template = "floor16", batch = NIGHT_BATCH, timeoutTicks = LEAVE_IT_TICKS + 100)
    public static void an_owned_pet_needs_a_slip_for_mushrooms_too(GameTestHelper helper) {
        holding(worker(helper, new BlockPos(4, STAND, 4)), Items.WOODEN_HOE);
        BlockPos mushroom = new BlockPos(7, STAND, 4);
        helper.setBlock(mushroom, Blocks.RED_MUSHROOM);

        helper.runAtTickTime(LEAVE_IT_TICKS, () -> {
            helper.assertBlockPresent(Blocks.RED_MUSHROOM, mushroom);
            helper.succeed();
        });
    }

    /**
     * A slip dies with the pet. Everything else it was carrying is kept in the doll, but a
     * job it agreed to do is between that pet and that day's board — a revived pet turning
     * up still owing work from a board that has long since moved on is nobody's second
     * chance. The pet lets the slip go before it drops the doll, and the order is the whole
     * of the rule.
     */
    @GameTest(template = "floor8", batch = DAY_BATCH, timeoutTicks = DEATH_TICKS)
    public static void a_slip_does_not_go_into_the_doll(GameTestHelper helper) {
        AbstractPet pet = holding(worker(helper, new BlockPos(3, STAND, 3)), Items.WOODEN_HOE);
        pet.setTask(weeding());
        helper.assertTrue(pet.getTask().isPresent(), "the pet would not take the slip it was handed");
        pet.hurt(pet.damageSources().generic(), pet.getMaxHealth() * 2.0F);

        helper.runAtTickTime(DEATH_TICKS / 2, () -> {
            ItemStack doll = GameTestKit.dollNear(helper, new BlockPos(3, STAND, 3), InitItems.SHISA_DOLL.get());
            boolean carriesSlip = PetDollData.readPetData(doll)
                .map(data -> data.contains("Task"))
                .orElse(false);
            helper.assertFalse(carriesSlip, "the doll came away with the slip still on it");
            helper.succeed();
        });
    }

    /** A weeding slip as a board would hand one out. */
    private static PetTask weeding() {
        Identifier farmer = InitRegistry.PET_JOB_REGISTRY.getKey(InitRegistry.FARMER.get());
        return new PetTask(PetTaskTypeData.WEEDING, farmer, PetWorkCounters.WEED, PetTask.NO_ICON, 8,
            PetTaskTypeData.reward(PetTaskTypeData.WEEDING), 0);
    }
}
