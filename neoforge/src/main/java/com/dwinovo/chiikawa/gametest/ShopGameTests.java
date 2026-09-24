package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.carries;
import static com.dwinovo.chiikawa.gametest.GameTestKit.holding;
import static com.dwinovo.chiikawa.gametest.GameTestKit.player;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;
import static com.dwinovo.chiikawa.gametest.GameTestKit.wildWorker;
import static com.dwinovo.chiikawa.gametest.GameTestKit.worker;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitBlocks;
import com.dwinovo.chiikawa.network.ShopPayloads.ShopTradePayload;
import com.dwinovo.chiikawa.network.ShopServerPacketHandler;
import com.dwinovo.chiikawa.shop.Wallet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Spending. A pet that earns and never spends is a pet with a growing pile of emeralds
 * and nothing to show for it, so the half of the loop that goes the other way gets cases
 * of its own.
 */
@GameTestHolder(Constants.MOD_ID)
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
            helper.assertTrue(Wallet.count(pet.getBackpack()) < WAGES, Component.literal("the pet never spent a thing"));
            // What a Shisa fancies: something bottled to drink.
            helper.assertTrue(carries(pet, Items.HONEY_BOTTLE) || carries(pet, Items.MILK_BUCKET),
                Component.literal("the pet came away from the counter with money gone and nothing in its bag"));
        });
    }

    /**
     * Money is what the currency tag holds, and nothing else: emeralds, as the mod ships, so
     * a diamond buys nothing. The shop pays out in the tag's first item. A pack that puts
     * something else in the tag changes all of this, and what slips pay, in one place.
     */
    @GameTest(template = "floor8", batch = BATCH)
    public static void money_is_what_the_currency_tag_holds(GameTestHelper helper) {
        helper.assertTrue(Wallet.isMoney(new ItemStack(Items.EMERALD)), Component.literal("an emerald is not money"));
        helper.assertFalse(Wallet.isMoney(new ItemStack(Items.DIAMOND)), Component.literal("a diamond is money though no tag says so"));
        helper.assertTrue(Wallet.coins(3).is(Items.EMERALD) && Wallet.coins(3).getCount() == 3,
            Component.literal("the shop pays out in " + Wallet.coins(3)));
        helper.succeed();
    }

    /** The owner's side of the counter: money out, goods in. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 100)
    public static void an_owner_buys_across_the_counter(GameTestHelper helper) {
        BlockPos counter = new BlockPos(4, STAND, 4);
        helper.setBlock(counter, InitBlocks.SHOP.get());
        ServerPlayer customer = atTheCounter(helper, counter);
        customer.getInventory().add(new ItemStack(Items.EMERALD, WAGES));

        ShopServerPacketHandler.handleTrade(
            new ShopTradePayload(helper.absolutePos(counter), BuiltInRegistries.ITEM.getKey(Items.COOKIE), true),
            customer);

        helper.assertTrue(customer.getInventory().contains(new ItemStack(Items.COOKIE)), Component.literal("no cookie"));
        helper.assertTrue(Wallet.count(customer.getInventory()) == WAGES - 1, Component.literal("the price was not what it said"));
        helper.succeed();
    }

    /** And the other way: what the farm brought in, turned back into money. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 100)
    public static void an_owner_sells_the_harvest(GameTestHelper helper) {
        BlockPos counter = new BlockPos(4, STAND, 4);
        helper.setBlock(counter, InitBlocks.SHOP.get());
        ServerPlayer customer = atTheCounter(helper, counter);
        customer.getInventory().add(new ItemStack(Items.WHEAT, 3));

        ShopServerPacketHandler.handleTrade(
            new ShopTradePayload(helper.absolutePos(counter), BuiltInRegistries.ITEM.getKey(Items.WHEAT), false),
            customer);

        helper.assertTrue(Wallet.count(customer.getInventory()) > 0, Component.literal("the shop paid nothing for the wheat"));
        helper.succeed();
    }

    /**
     * Shouted from across the field, the order does not get taken. The screen is a picture
     * of what the server had a moment ago; the counter is where the deal happens.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = 100)
    public static void a_customer_across_the_field_is_not_served(GameTestHelper helper) {
        BlockPos counter = new BlockPos(2, STAND, 2);
        helper.setBlock(counter, InitBlocks.SHOP.get());
        ServerPlayer customer = player(helper);
        BlockPos away = helper.absolutePos(new BlockPos(15, STAND, 15));
        customer.teleportTo(away.getX() + 0.5, away.getY(), away.getZ() + 0.5);
        customer.getInventory().add(new ItemStack(Items.EMERALD, WAGES));

        ShopServerPacketHandler.handleTrade(
            new ShopTradePayload(helper.absolutePos(counter), BuiltInRegistries.ITEM.getKey(Items.COOKIE), true),
            customer);

        helper.assertTrue(Wallet.count(customer.getInventory()) == WAGES, Component.literal("the shop served someone out of reach"));
        helper.succeed();
    }

    /** Standing where a customer stands. */
    private static ServerPlayer atTheCounter(GameTestHelper helper, BlockPos counter) {
        ServerPlayer customer = player(helper);
        BlockPos beside = helper.absolutePos(counter.south());
        customer.teleportTo(beside.getX() + 0.5, beside.getY(), beside.getZ() + 0.5);
        return customer;
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
            helper.assertTrue(Wallet.count(pet.getBackpack()) == WAGES, Component.literal("a wild pet went shopping"));
            helper.succeed();
        });
    }
}
