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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
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
@PrefixGameTestTemplate(false)
public final class GiftGameTests {
    private static final String BATCH = "chiikawa_gift";
    private static final int WALK_TICKS = 1200;

    private static final int STAND = 2;
    private static final int POCKET_MONEY = 5;
    /** Long enough for a pet that meant to give something to have set off and arrived. */
    private static final int NOTHING_TICKS = 600;
    private static final int SHOP_TICKS = 3600;
    private static final int WAGES = 8;
    private static final int TEN = 10;
    /** How long a pet is watched with its owner away, how far away that is, and how long to see it rest. */
    private static final int AWAY_TICKS = 300;
    private static final int AWAY_BLOCKS = 30;
    private static final int SETTLED_TICKS = 40;
    /** How much money a bag that is otherwise full has room for. */
    private static final int ROOM = 4;
    /** Long enough for a pet beside its owner to give a present, and to give a second if it were going to. */
    private static final int THINKING_TICKS = 200;

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
                "the owner never got the cake");
            helper.assertTrue(pet.getPendingGift().isEmpty(),
                "the pet handed the cake over and kept hold of it too");
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
            List<ItemStack> given = owner.getInventory().getNonEquipmentItems().stream()
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
            ItemStack given = owner.getInventory().getNonEquipmentItems().stream().filter(stack -> stack.is(InitItems.FILM.get()))
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
     * A pet that roams free gives a present only to an owner who is at home: with the owner well
     * outside the area it acts in, the cookie stays in the bag; once the owner stands near, it is
     * given.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = AWAY_TICKS + WALK_TICKS)
    public static void a_free_pet_gives_its_present_once_its_owner_is_home(GameTestHelper helper) {
        ServerPlayer owner = ownerAcross(helper);
        teleportAway(helper, owner);
        AbstractPet pet = ownedBy(helper, InitEntity.USAGI_PET.get(), owner);
        pet.getBackpack().setItem(2, new ItemStack(Items.COOKIE, 2));
        boolean[] home = {false};

        helper.onEachTick(() -> {
            if (!home[0]) {
                helper.assertTrue(pet.getPendingGift().isEmpty(), "the pet set off with a present for an owner far from home");
                helper.assertFalse(owner.getInventory().contains(new ItemStack(Items.COOKIE)), "the owner far away was given a cookie");
                helper.assertTrue(count(pet, Items.COOKIE) == 2, "the pet has " + count(pet, Items.COOKIE) + " cookies, not the two");
            }
        });
        helper.runAfterDelay(AWAY_TICKS, () -> {
            home[0] = true;
            BlockPos near = helper.absolutePos(new BlockPos(4, STAND, 3));
            owner.teleportTo(near.getX() + 0.5, near.getY(), near.getZ() + 0.5);
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(home[0], "the owner has not come home");
            helper.assertTrue(owner.getInventory().countItem(Items.COOKIE) == 1, "the owner at home was not given a cookie");
        });
    }

    /**
     * Following, a pet is with its owner wherever that is: an owner who is further off than a free
     * pet would walk a present to is no reason to wait.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WALK_TICKS)
    public static void a_following_pet_still_gives_its_present_to_an_owner_far_away(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        BlockPos corner = helper.absolutePos(new BlockPos(14, STAND, 14));
        owner.teleportTo(corner.getX() + 0.5, corner.getY(), corner.getZ() + 0.5);
        AbstractPet pet = ownedBy(helper, InitEntity.USAGI_PET.get(), owner);
        pet.setPetDirective(PetDirective.FOLLOW);
        pet.getBackpack().setItem(2, new ItemStack(Items.COOKIE, 2));

        helper.succeedWhen(() -> helper.assertTrue(owner.getInventory().countItem(Items.COOKIE) == 1,
            "the owner was not given a cookie"));
    }

    /**
     * An owner who leaves the pet's home on the way ends the errand: what the pet was carrying
     * goes back in its bag, and it does not set off again while the owner is away.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WALK_TICKS)
    public static void a_present_is_put_back_when_the_owner_leaves_home(GameTestHelper helper) {
        ServerPlayer owner = ownerAcross(helper);
        AbstractPet pet = ownedBy(helper, InitEntity.USAGI_PET.get(), owner);
        pet.getBackpack().setItem(2, new ItemStack(Items.COOKIE, 2));
        boolean[] left = {false};
        int[] settled = {0};

        helper.succeedWhen(() -> {
            if (!left[0] && !pet.getPendingGift().isEmpty()) {
                left[0] = true;
                teleportAway(helper, owner);
            }
            helper.assertTrue(left[0], "the pet never set off with a present");
            helper.assertTrue(pet.getPendingGift().isEmpty(), "the pet still carries the cookie");
            helper.assertTrue(count(pet, Items.COOKIE) == 2, "the pet has " + count(pet, Items.COOKIE) + " cookies, not the two");
            helper.assertFalse(owner.getInventory().contains(new ItemStack(Items.COOKIE)), "the owner was given the cookie");
            helper.assertTrue(++settled[0] > SETTLED_TICKS, "too soon to say it does not set off again");
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

    /**
     * One present, then a rest: with the owner right beside it and five cookies in its bag, a pet
     * hands over one and thinks of no other, however long it is left. The errand must not start
     * again in the tick after the hand-over, before the pet has chosen what to do anew.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = NOTHING_TICKS)
    public static void a_pet_gives_one_present_and_then_rests(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        BlockPos beside = helper.absolutePos(new BlockPos(4, STAND, 3));
        owner.teleportTo(beside.getX() + 0.5, beside.getY(), beside.getZ() + 0.5);
        AbstractPet pet = ownedBy(helper, InitEntity.USAGI_PET.get(), owner);
        pet.getBackpack().setItem(2, new ItemStack(Items.COOKIE, 5));

        helper.runAfterDelay(THINKING_TICKS, () -> {
            helper.assertTrue(owner.getInventory().countItem(Items.COOKIE) == 1,
                "the owner has " + owner.getInventory().countItem(Items.COOKIE) + " cookies, not the one");
            helper.assertTrue(count(pet, Items.COOKIE) == 4, "the pet has " + count(pet, Items.COOKIE) + " cookies left, not four");
            helper.assertTrue(pet.getPendingGift().isEmpty(), "the pet is carrying another present");
            helper.succeed();
        });
    }

    /**
     * The owner, across the yard from where the pet stands, so that the pet has a walk to make:
     * well inside the area a free pet acts in round its home.
     */
    private static ServerPlayer ownerAcross(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        BlockPos across = helper.absolutePos(new BlockPos(10, STAND, 10));
        owner.teleportTo(across.getX() + 0.5, across.getY(), across.getZ() + 0.5);
        return owner;
    }

    /** Far outside the area a pet acts in, in the same level. */
    private static void teleportAway(GameTestHelper helper, ServerPlayer owner) {
        BlockPos far = helper.absolutePos(new BlockPos(8, STAND, 8)).offset(AWAY_BLOCKS, 0, 0);
        owner.teleportTo(far.getX() + 0.5, far.getY(), far.getZ() + 0.5);
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
            "the pet did not take the money it was handed");
        helper.assertTrue(carries(pet, Items.EMERALD), "the money went somewhere other than its bag");
        helper.succeed();
    }

    /** In survival the emeralds leave the owner's hand: what the pet gets, the owner no longer has. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 100)
    public static void money_handed_over_in_survival_leaves_the_owners_hand(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        owner.setGameMode(GameType.SURVIVAL);
        AbstractPet pet = wildPet(helper, new BlockPos(3, STAND, 3));
        pet.tame(owner);
        owner.setShiftKeyDown(false);
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.EMERALD, TEN));

        pet.mobInteract(owner, InteractionHand.MAIN_HAND);

        helper.assertTrue(Wallet.count(pet.getBackpack()) == TEN, "the pet has " + Wallet.count(pet.getBackpack()) + " emeralds, not ten");
        helper.assertTrue(owner.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(),
            "the owner still holds " + owner.getItemInHand(InteractionHand.MAIN_HAND));
        helper.succeed();
    }

    /** With room for only some of it, the owner keeps the rest, and not an emerald is made or lost. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 100)
    public static void money_that_does_not_fit_stays_with_the_owner(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        owner.setGameMode(GameType.SURVIVAL);
        AbstractPet pet = wildPet(helper, new BlockPos(3, STAND, 3));
        pet.tame(owner);
        for (int slot = 0; slot < pet.getBackpack().getContainerSize(); slot++) {
            pet.getBackpack().setItem(slot, new ItemStack(Items.DIRT, Items.DIRT.getDefaultMaxStackSize()));
        }
        pet.getBackpack().setItem(4, new ItemStack(Items.EMERALD, Items.EMERALD.getDefaultMaxStackSize() - ROOM));
        owner.setShiftKeyDown(false);
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.EMERALD, TEN));

        pet.mobInteract(owner, InteractionHand.MAIN_HAND);

        int held = owner.getItemInHand(InteractionHand.MAIN_HAND).getCount();
        helper.assertTrue(Wallet.count(pet.getBackpack()) == Items.EMERALD.getDefaultMaxStackSize(),
            "the pet has " + Wallet.count(pet.getBackpack()) + " emeralds");
        helper.assertTrue(held == TEN - ROOM, "the owner is left with " + held + " emeralds, not " + (TEN - ROOM));
        helper.succeed();
    }

    /** In creative the hand is left as it was, and the pet still gets the money. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 100)
    public static void money_handed_over_in_creative_leaves_the_owners_hand_alone(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        owner.setGameMode(GameType.CREATIVE);
        AbstractPet pet = wildPet(helper, new BlockPos(3, STAND, 3));
        pet.tame(owner);
        owner.setShiftKeyDown(false);
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.EMERALD, TEN));

        pet.mobInteract(owner, InteractionHand.MAIN_HAND);

        helper.assertTrue(Wallet.count(pet.getBackpack()) == TEN, "the pet has " + Wallet.count(pet.getBackpack()) + " emeralds, not ten");
        helper.assertTrue(owner.getItemInHand(InteractionHand.MAIN_HAND).getCount() == TEN, "the creative owner's hand changed");
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
            "somebody else's pet took money from a stranger");
        helper.succeed();
    }
}
