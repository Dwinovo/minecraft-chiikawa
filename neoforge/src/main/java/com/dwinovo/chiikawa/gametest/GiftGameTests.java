package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.carries;
import static com.dwinovo.chiikawa.gametest.GameTestKit.count;
import static com.dwinovo.chiikawa.gametest.GameTestKit.player;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;
import static com.dwinovo.chiikawa.gametest.GameTestKit.wildPet;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.camera.FilmRolls;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.init.InitBlocks;
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.item.FilmItem;
import com.dwinovo.chiikawa.photo.FilmRoll;
import com.dwinovo.chiikawa.photo.PhotoData;
import com.dwinovo.chiikawa.photo.PhotoSubject;
import com.dwinovo.chiikawa.shop.Wallet;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
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
    /** Long enough for a pet that meant to give something to have set off and arrived. */
    private static final int NOTHING_TICKS = 600;
    private static final int SHOP_TICKS = 3600;
    private static final int WAGES = 8;

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

    /**
     * A pet with something in its bag and no present given for a while thinks of its owner and takes
     * one of them over: a single cookie, out of the bag and into the owner's hands.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WALK_TICKS)
    public static void a_pet_with_a_cookie_in_its_bag_gives_it_to_its_owner(GameTestHelper helper) {
        ServerPlayer owner = ownerAcross(helper);
        AbstractPet pet = ownedBy(helper, InitEntity.USAGI_PET.get(), owner);
        pet.getBackpack().setItem(AbstractPet.BAG_SLOT + 1, new ItemStack(Items.COOKIE, 3));

        helper.succeedWhen(() -> {
            helper.assertTrue(owner.getInventory().countItem(Items.COOKIE) == 1,
                "the owner has " + owner.getInventory().countItem(Items.COOKIE) + " cookies, not the one");
            helper.assertTrue(count(pet, Items.COOKIE) == 2, "the pet has " + count(pet, Items.COOKIE) + " cookies left");
            helper.assertTrue(pet.getPendingGift().isEmpty(), "the pet handed the cookie over and kept hold of it too");
            helper.assertTrue(pet.getBrain().hasMemoryValue(InitMemory.GIFT_COOLDOWN.get()),
                "the pet is ready to think of a second present at once");
        });
    }

    /**
     * What a pet keeps, and what it is not done with, is not a present: a camera, its money and a
     * roll of film it has still to finish.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = NOTHING_TICKS + 20)
    public static void a_pet_with_only_a_camera_money_and_an_unfinished_roll_gives_nothing(GameTestHelper helper) {
        ServerPlayer owner = ownerAcross(helper);
        AbstractPet pet = ownedBy(helper, InitEntity.HACHIWARE_PET.get(), owner);
        pet.getBackpack().setItem(2, new ItemStack(InitItems.CAMERA.get()));
        pet.getBackpack().setItem(3, new ItemStack(Items.EMERALD, POCKET_MONEY));
        pet.getBackpack().setItem(4, FilmRolls.begin(InitItems.FILM.get(), FilmRoll.MOST_EXPOSURES, shot()));

        helper.runAfterDelay(NOTHING_TICKS, () -> {
            // The handbook is the advancement's, on the owner's joining.
            List<ItemStack> given = owner.getInventory().items.stream()
                .filter(stack -> !stack.isEmpty() && !stack.is(InitItems.HANDBOOK.get())).toList();
            helper.assertTrue(given.isEmpty(), "the owner was given something: " + given);
            helper.assertTrue(pet.getPendingGift().isEmpty(), "the pet was carrying something over");
            helper.assertFalse(pet.getBrain().hasMemoryValue(InitMemory.GIFT_COOLDOWN.get()), "the pet gave a present");
            helper.assertTrue(carries(pet, InitItems.CAMERA.get()) && Wallet.count(pet.getBackpack()) == POCKET_MONEY
                && carries(pet, InitItems.FILM.get()), "the pet no longer has all that it had");
            helper.succeed();
        });
    }

    /** A roll that is full is an album to give, though film is something Hachiware keeps. */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WALK_TICKS)
    public static void a_full_roll_of_film_is_given_to_the_owner(GameTestHelper helper) {
        ServerPlayer owner = ownerAcross(helper);
        AbstractPet pet = ownedBy(helper, InitEntity.HACHIWARE_PET.get(), owner);
        pet.getBackpack().setItem(2, new ItemStack(InitItems.CAMERA.get()));
        ItemStack roll = FilmRolls.begin(InitItems.FILM.get(), 2, shot());
        FilmRolls.add(roll, shot());
        pet.getBackpack().setItem(3, roll);

        helper.succeedWhen(() -> {
            ItemStack given = owner.getInventory().items.stream().filter(stack -> stack.is(InitItems.FILM.get()))
                .findFirst().orElse(ItemStack.EMPTY);
            helper.assertFalse(given.isEmpty(), "the owner never got the roll");
            helper.assertTrue(FilmItem.rollOf(given).full(), "the owner was given a roll that was not full");
            helper.assertTrue(carries(pet, InitItems.CAMERA.get()), "the camera went with it");
        });
    }

    /** What a pet buys is its own: shopping makes no present, however many times it is done. */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = SHOP_TICKS)
    public static void shopping_never_makes_a_present(GameTestHelper helper) {
        helper.setBlock(new BlockPos(9, STAND, 4), InitBlocks.SHOP.get());
        AbstractPet pet = GameTestKit.holding(GameTestKit.worker(helper, new BlockPos(4, STAND, 4)), Items.WOODEN_HOE);
        pet.getBackpack().addItem(new ItemStack(Items.EMERALD, WAGES));

        helper.succeedWhen(() -> {
            // Back to the counter as soon as it has left it.
            pet.getBrain().eraseMemory(InitMemory.SHOP_COOLDOWN.get());
            helper.assertTrue(pet.getPendingGift().isEmpty(), "shopping left the pet with a present: " + pet.getPendingGift());
            helper.assertTrue(Wallet.count(pet.getBackpack()) < WAGES, "the pet has not spent anything yet");
        });
    }

    /**
     * Told to sit on the way, the pet puts what it was carrying back in its bag: nobody was given
     * it, and so the pet is not tired of giving.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WALK_TICKS)
    public static void a_present_is_put_back_when_the_walk_is_abandoned(GameTestHelper helper) {
        ServerPlayer owner = ownerAcross(helper);
        AbstractPet pet = ownedBy(helper, InitEntity.USAGI_PET.get(), owner);
        pet.getBackpack().setItem(2, new ItemStack(Items.COOKIE, 2));
        boolean[] told = {false};

        helper.succeedWhen(() -> {
            if (!told[0] && !pet.getPendingGift().isEmpty()) {
                told[0] = true;
                pet.setPetDirective(PetDirective.STAY);
            }
            helper.assertTrue(told[0], "the pet never set off with a present");
            helper.assertTrue(pet.getPendingGift().isEmpty(), "the pet still carries the cookie");
            helper.assertTrue(count(pet, Items.COOKIE) == 2, "the pet has " + count(pet, Items.COOKIE) + " cookies, not the two");
            helper.assertFalse(owner.getInventory().contains(new ItemStack(Items.COOKIE)), "the owner was given the cookie");
            helper.assertFalse(pet.getBrain().hasMemoryValue(InitMemory.GIFT_COOLDOWN.get()), "a present never given tired the pet");
        });
    }

    /** The owner, across the yard from where the pet stands, so that the pet has a walk to make. */
    private static ServerPlayer ownerAcross(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        BlockPos across = helper.absolutePos(new BlockPos(12, STAND, 12));
        owner.teleportTo(across.getX() + 0.5, across.getY(), across.getZ() + 0.5);
        return owner;
    }

    private static AbstractPet ownedBy(GameTestHelper helper, EntityType<? extends AbstractPet> type, ServerPlayer owner) {
        AbstractPet pet = GameTestKit.wild(helper, type, new BlockPos(3, STAND, 3));
        pet.tame(owner);
        pet.setPetDirective(PetDirective.FREE);
        return pet;
    }

    private static PhotoData shot() {
        return new PhotoData(UUID.randomUUID(), PhotoSubject.SCENERY, "", 1, "Hachiware");
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
