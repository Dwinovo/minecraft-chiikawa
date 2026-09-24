package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.carries;
import static com.dwinovo.chiikawa.gametest.GameTestKit.player;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;
import static com.dwinovo.chiikawa.gametest.GameTestKit.wildPet;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.shop.Wallet;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Presents, and pocket money: the two ways things pass between a pet and the person it
 * belongs to without either of them going near a shop counter.
 */
@GameTestHolder(Constants.MOD_ID)
public final class GiftGameTests {
    private static final String BATCH = "chiikawa_gift";
    private static final int WALK_TICKS = 1200;

    private static final int STAND = 2;
    private static final int POCKET_MONEY = 5;

    @BeforeBatch(batch = BATCH)
    public static void settle(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    /** With something bought for its owner, the pet goes and finds them with it. */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WALK_TICKS)
    public static void a_pet_takes_its_present_to_its_owner(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        BlockPos across = helper.absolutePos(new BlockPos(12, STAND, 12));
        owner.teleportTo(across.getX() + 0.5, across.getY(), across.getZ() + 0.5);

        AbstractPet pet = wildPet(helper, new BlockPos(3, STAND, 3));
        pet.tame(owner);
        pet.setPetDirective(PetDirective.FREE);
        pet.setPendingGift(new ItemStack(Items.CAKE));

        helper.succeedWhen(() -> {
            helper.assertTrue(owner.getInventory().contains(new ItemStack(Items.CAKE)),
                Component.literal("the owner never got the cake"));
            helper.assertTrue(pet.getPendingGift().isEmpty(),
                Component.literal("the pet handed the cake over and kept hold of it too"));
        });
    }

    /** Emeralds pressed into its paws go into its bag, for it to spend as it sees fit. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 100)
    public static void an_owner_can_hand_a_pet_pocket_money(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        AbstractPet pet = wildPet(helper, new BlockPos(3, STAND, 3));
        pet.tame(owner);
        owner.setShiftKeyDown(false);
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.EMERALD, POCKET_MONEY));

        pet.mobInteract(owner, InteractionHand.MAIN_HAND);

        helper.assertTrue(Wallet.count(pet.getBackpack()) == POCKET_MONEY,
            Component.literal("the pet did not take the money it was handed"));
        helper.assertTrue(carries(pet, Items.EMERALD), Component.literal("the money went somewhere other than its bag"));
        helper.succeed();
    }

    /** A stranger's emeralds are their own business. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 100)
    public static void a_stranger_cannot_hand_a_pet_money(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        ServerPlayer stranger = player(helper);
        AbstractPet pet = wildPet(helper, new BlockPos(3, STAND, 3));
        pet.tame(owner);
        stranger.setShiftKeyDown(false);
        stranger.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.EMERALD, POCKET_MONEY));

        pet.mobInteract(stranger, InteractionHand.MAIN_HAND);

        helper.assertTrue(Wallet.count(pet.getBackpack()) == 0,
            Component.literal("somebody else's pet took money from a stranger"));
        helper.succeed();
    }
}
