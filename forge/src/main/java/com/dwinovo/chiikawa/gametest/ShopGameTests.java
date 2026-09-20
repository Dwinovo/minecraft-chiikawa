package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.carries;
import static com.dwinovo.chiikawa.gametest.GameTestKit.holding;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;
import static com.dwinovo.chiikawa.gametest.GameTestKit.wildWorker;
import static com.dwinovo.chiikawa.gametest.GameTestKit.worker;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitBlocks;
import com.dwinovo.chiikawa.shop.Wallet;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Spending. A pet that earns and never spends is a pet with a growing pile of emeralds
 * and nothing to show for it, so the half of the loop that goes the other way gets cases
 * of its own.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ShopGameTests {
    private static final String BATCH = "chiikawa_shop";
    private static final int SHOP_TICKS = 3600;
    private static final int LEAVE_IT_TICKS = 600;

    private static final int STAND = 2;
    /** Enough for anything on the list, so the case is about wanting rather than affording. */
    private static final int WAGES = 8;

    @BeforeBatch(batch = BATCH)
    public static void settle(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    /** Money in its bag and a counter across the yard: the pet goes and buys itself something. */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = SHOP_TICKS)
    public static void a_pet_with_wages_buys_itself_something(GameTestHelper helper) {
        helper.setBlock(new BlockPos(9, STAND, 4), InitBlocks.SHOP.get());
        AbstractPet pet = holding(worker(helper, new BlockPos(4, STAND, 4)), Items.WOODEN_HOE);
        pet.getBackpack().addItem(new ItemStack(Items.EMERALD, WAGES));

        helper.succeedWhen(() -> {
            helper.assertTrue(Wallet.count(pet.getBackpack()) < WAGES, "the pet never spent a thing");
            // What a Shisa fancies: something bottled to drink.
            helper.assertTrue(carries(pet, Items.HONEY_BOTTLE) || carries(pet, Items.MILK_BUCKET),
                "the pet came away from the counter with money gone and nothing in its bag");
        });
    }

    /**
     * A wild pet leaves the shop alone. It has an owner's wages in its bag only because a
     * case put them there; shopping is what a kept pet does with a day of its own.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = LEAVE_IT_TICKS + 100)
    public static void a_wild_pet_does_not_go_shopping(GameTestHelper helper) {
        helper.setBlock(new BlockPos(9, STAND, 4), InitBlocks.SHOP.get());
        AbstractPet pet = holding(wildWorker(helper, new BlockPos(4, STAND, 4)), Items.WOODEN_HOE);
        pet.getBackpack().addItem(new ItemStack(Items.EMERALD, WAGES));

        helper.runAtTickTime(LEAVE_IT_TICKS, () -> {
            helper.assertTrue(Wallet.count(pet.getBackpack()) == WAGES, "a wild pet went shopping");
            helper.succeed();
        });
    }
}
