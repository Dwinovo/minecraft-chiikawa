package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.MIDNIGHT;
import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.carries;
import static com.dwinovo.chiikawa.gametest.GameTestKit.count;
import static com.dwinovo.chiikawa.gametest.GameTestKit.player;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;
import static com.dwinovo.chiikawa.gametest.GameTestKit.wildPet;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.camera.FilmRolls;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.entity.brain.PetMind;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.Message;
import com.dwinovo.chiikawa.gifting.GiftLedger;
import com.dwinovo.chiikawa.gifting.GiftOccasion;
import com.dwinovo.chiikawa.gifting.GiftingSettings;
import com.dwinovo.chiikawa.init.InitBlocks;
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.item.FilmItem;
import com.dwinovo.chiikawa.photo.FilmRoll;
import com.dwinovo.chiikawa.photo.PhotoData;
import com.dwinovo.chiikawa.photo.PhotoSubject;
import com.dwinovo.chiikawa.shop.Wallet;
import com.mojang.datafixers.util.Either;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Presents, and pocket money: the two ways things pass between a pet and the person it
 * belongs to without either of them going near a shop counter.
 *
 * <p>A pet thinks of a present only because something happened (a treat, wages, a pass, a finished roll of film, a flower
 * found), by a chance, and a day after the last it gave. The cases that are about the walk, the hand-over and what is put back
 * are not about the chance, so most of them run under settings in which every occasion is certain and none is put off; the
 * ones about the numbers that ship have a batch of their own, with the settings as they are.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GiftGameTests {
    private static final String BATCH = "chiikawa_gift";
    private static final String FADE_BATCH = "chiikawa_gift_fade";
    private static final String BED_BATCH = "chiikawa_gift_bed";
    private static final String NIGHT_BATCH = "chiikawa_gift_bed_night";
    private static final String SHIPPED_BATCH = "chiikawa_gift_shipped";
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
    /** The least a rest of a game day leaves after a few hundred ticks of it. */
    private static final int RESTING = 20000;
    /** How long a present is remembered in the batch about forgetting it. */
    private static final int FADE_TICKS = 100;
    /** The time of day an owner wakes at in the morning, as the clock of the level has it: a day's start. */
    private static final long DAWN = 24000L;
    private static final int PETS = 4;
    private static final int DAILY_LIMIT = 3;

    /** The settings as the data pack has them, put back after a batch that tried others. */
    private static Optional<GiftingSettings> shipped = Optional.empty();

    @BeforeBatch(batch = BATCH)
    public static void settle(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
        tryOther(certain(Integer.MAX_VALUE));
    }

    @AfterBatch(batch = BATCH)
    public static void putTheSettingsBack(ServerLevel level) {
        GiftingSettings.replace(shipped);
    }

    @BeforeBatch(batch = FADE_BATCH)
    public static void forgetQuickly(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
        tryOther(certain(FADE_TICKS));
    }

    @AfterBatch(batch = FADE_BATCH)
    public static void putTheSettingsBackAfterForgetting(ServerLevel level) {
        GiftingSettings.replace(shipped);
    }

    @BeforeBatch(batch = BED_BATCH)
    public static void nightWithGiving(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, MIDNIGHT);
        tryOther(certain(Integer.MAX_VALUE));
    }

    @AfterBatch(batch = BED_BATCH)
    public static void putTheSettingsBackAfterTheNight(ServerLevel level) {
        GiftingSettings.replace(shipped);
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    @BeforeBatch(batch = NIGHT_BATCH)
    public static void nightAgainWithGiving(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, MIDNIGHT);
        tryOther(certain(Integer.MAX_VALUE));
    }

    @AfterBatch(batch = NIGHT_BATCH)
    public static void putTheSettingsBackAfterTheOtherNight(ServerLevel level) {
        GiftingSettings.replace(shipped);
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    @BeforeBatch(batch = SHIPPED_BATCH)
    public static void settleWithShippedSettings(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    /** Remembers the settings in force, and puts these in their place. */
    private static void tryOther(GiftingSettings settings) {
        shipped = GiftingSettings.current();
        GiftingSettings.replace(Optional.of(settings));
    }

    /**
     * The shipped settings with every occasion certain and none put off, and a pet forgetting a present after this long: what
     * a case about what a pet does with a present in mind wants, which is not to wait on a roll of the dice. The gifts are
     * the shipped ones and wheat seeds, a sword and arrows besides, which no pet gives from its bag as a rule: a case about
     * what a pet never gives needs a list that gives them, to show it is the pet that does not.
     */
    private static GiftingSettings certain(int fadeTicks) {
        GiftingSettings data = GiftingSettings.current().orElseThrow();
        Map<GiftOccasion, GiftingSettings.OccasionSettings> sure = new EnumMap<>(GiftOccasion.class);
        for (GiftOccasion occasion : GiftOccasion.values()) {
            sure.put(occasion, new GiftingSettings.OccasionSettings(1.0F, 0));
        }
        List<GiftingSettings.Gift> gifts = new ArrayList<>(data.gifts());
        for (Item item : List.of(Items.WHEAT_SEEDS, Items.IRON_SWORD, Items.ARROW)) {
            gifts.add(new GiftingSettings.Gift(Either.left(item), 1, List.of()));
        }
        return new GiftingSettings(data.cooldownTicks(), data.ownerDailyLimit(), fadeTicks, sure, gifts, data.never());
    }

    /** Something happened that may put a present in the pet's mind, as it would be told. */
    private static void thinkOf(AbstractPet pet, GiftOccasion occasion) {
        PetMind.post(pet, new Message.Occasion(occasion, Optional.empty()));
    }

    /**
     * The settings that ship, as the game loaded them, say what the design says: a pet gives a present a day, an owner
     * is given three, a present is remembered for a day, and each occasion has the chance it was given.
     */
    @GameTest(template = "floor8", batch = SHIPPED_BATCH, timeoutTicks = 20)
    public static void the_shipped_settings_are_the_ones_the_design_says(GameTestHelper helper) {
        GiftingSettings settings = GiftingSettings.current().orElseThrow(() -> new AssertionError("no gifting settings were loaded"));

        helper.assertTrue(settings.cooldownTicks() == 24000 && settings.ownerDailyLimit() == DAILY_LIMIT && settings.fadeTicks() == 24000,
            "a pet gives one present a day, an owner is given three and a present is remembered for a day: " + settings);
        Map<GiftOccasion, Float> chances = new EnumMap<>(GiftOccasion.class);
        settings.occasions().forEach((occasion, how) -> chances.put(occasion, how.chance()));
        helper.assertTrue(chances.equals(Map.of(GiftOccasion.FOUND_SOMETHING, 0.5F, GiftOccasion.GOT_PAID, 0.2F,
            GiftOccasion.PASSED_EXAM, 0.5F, GiftOccasion.WAS_TREATED, 0.3F, GiftOccasion.FILM_FULL, 1.0F)), "the chances are " + chances);
        helper.assertTrue(settings.occasion(GiftOccasion.WAS_TREATED).orElseThrow().delayTicks() == 1200,
            "a treat is answered a minute later");
        helper.succeed();
    }

    /**
     * Rakko gives sweets and Furuhonya books, as the series has them, and a pet that is known for neither does not give a
     * book it happens to have. Under the settings as they ship; a finished roll of film is the occasion that is certain.
     */
    @GameTest(template = "floor16", batch = SHIPPED_BATCH, timeoutTicks = WALK_TICKS)
    public static void a_rakko_gives_sweets_and_a_furuhonya_books_and_a_chiikawa_neither(GameTestHelper helper) {
        ServerPlayer rakkosOwner = ownerAcross(helper);
        ServerPlayer furuhonyasOwner = ownerAcross(helper);
        ServerPlayer chiikawasOwner = ownerAcross(helper);
        AbstractPet rakko = ownedBy(helper, InitEntity.RAKKO_PET.get(), rakkosOwner);
        AbstractPet furuhonya = ownedBy(helper, InitEntity.FURUHONYA_PET.get(), furuhonyasOwner);
        AbstractPet chiikawa = ownedBy(helper, InitEntity.CHIIKAWA_PET.get(), chiikawasOwner);
        rakko.getBackpack().setItem(2, new ItemStack(Items.CAKE));
        furuhonya.getBackpack().setItem(2, new ItemStack(Items.BOOK));
        chiikawa.getBackpack().setItem(2, new ItemStack(Items.BOOK));
        List.of(rakko, furuhonya, chiikawa).forEach(pet -> thinkOf(pet, GiftOccasion.FILM_FULL));

        helper.succeedWhen(() -> {
            helper.assertTrue(rakkosOwner.getInventory().countItem(Items.CAKE) == 1, "Rakko never gave its sweet");
            helper.assertTrue(furuhonyasOwner.getInventory().countItem(Items.BOOK) == 1, "Furuhonya never gave its book");
            helper.assertTrue(chiikawa.getPendingGift().isEmpty() && chiikawasOwner.getInventory().countItem(Items.BOOK) == 0
                && carries(chiikawa, Items.BOOK), "Chiikawa gave a book, which is not what it is known for");
        });
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
     * Fed a cookie, which Usagi likes, the pet thinks of its owner and takes over one of the cookies it has: a single one,
     * out of the bag and into the owner's hands, and rests from giving for a game day.
     *
     * <p>(A) Before 0.1.4 a pet gave a present whenever its short cooldown was over and it had something in the bag, so the
     * case had only to give it a cookie; now it gives because something happened, and a treat is what happened, and the
     * rest after a present is a day and not a quarter of one.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WALK_TICKS)
    public static void a_pet_treated_with_a_cookie_gives_one_of_its_cookies_back(GameTestHelper helper) {
        ServerPlayer owner = ownerAcross(helper);
        AbstractPet pet = ownedBy(helper, InitEntity.USAGI_PET.get(), owner);
        pet.getBackpack().setItem(AbstractPet.BAG_SLOT + 1, new ItemStack(Items.COOKIE, 3));
        owner.setShiftKeyDown(false);
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COOKIE));

        pet.mobInteract(owner, InteractionHand.MAIN_HAND);

        // The stand-in owner is in creative whatever the case says, and keeps what it feeds: what it is given is the pet's.
        owner.getInventory().clearContent();
        helper.succeedWhen(() -> {
            helper.assertTrue(owner.getInventory().countItem(Items.COOKIE) == 1,
                "the owner has " + owner.getInventory().countItem(Items.COOKIE) + " cookies, not the one");
            helper.assertTrue(count(pet, Items.COOKIE) == 2, "the pet has " + count(pet, Items.COOKIE) + " cookies left");
            helper.assertTrue(pet.getPendingGift().isEmpty(), "the pet handed the cookie over and kept hold of it too");
            long resting = pet.getBrain().getTimeUntilExpiry(InitMemory.GIFT_COOLDOWN.get());
            helper.assertTrue(resting > RESTING, "the pet rests from giving for " + resting + " ticks, not a day");
            helper.assertTrue(pet.giftIntentions().held().isEmpty(), "the pet still has the present in mind");
        });
    }

    /**
     * What a pet keeps, and what it is not done with, is not a present: a camera, its money and a roll of film it has
     * still to finish, with a present in mind and nothing else in the bag.
     *
     * <p>(A) The pet is given an occasion to think of a present, which it was not before: without one it would give nothing
     * for want of a reason and the case would show nothing.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = NOTHING_TICKS + 20)
    public static void a_pet_with_only_a_camera_money_and_an_unfinished_roll_gives_nothing(GameTestHelper helper) {
        ServerPlayer owner = ownerAcross(helper);
        AbstractPet pet = ownedBy(helper, InitEntity.HACHIWARE_PET.get(), owner);
        pet.getBackpack().setItem(2, new ItemStack(InitItems.CAMERA.get()));
        pet.getBackpack().setItem(3, new ItemStack(Items.EMERALD, POCKET_MONEY));
        pet.getBackpack().setItem(4, FilmRolls.begin(InitItems.FILM.get(), FilmRoll.MOST_EXPOSURES, shot()));
        thinkOf(pet, GiftOccasion.GOT_PAID);

        helper.runAfterDelay(NOTHING_TICKS, () -> {
            // The handbook is the advancement's, on the owner's joining.
            List<ItemStack> given = owner.getInventory().items.stream()
                .filter(stack -> !stack.isEmpty() && !stack.is(InitItems.HANDBOOK.get())).toList();
            helper.assertTrue(given.isEmpty(), "the owner was given something: " + given);
            helper.assertTrue(pet.getPendingGift().isEmpty(), "the pet was carrying something over");
            helper.assertFalse(pet.getBrain().hasMemoryValue(InitMemory.GIFT_COOLDOWN.get()), "the pet gave a present");
            helper.assertTrue(pet.giftIntentions().held().isPresent(), "the pet gave up the thought of a present, though it could not give one");
            helper.assertTrue(carries(pet, InitItems.CAMERA.get()) && Wallet.count(pet.getBackpack()) == POCKET_MONEY
                && carries(pet, InitItems.FILM.get()), "the pet no longer has all that it had");
            helper.succeed();
        });
    }

    /**
     * The shot that fills a roll of film is an occasion, and the roll itself is what the pet has set aside: an album to
     * give, though film is something Hachiware keeps.
     *
     * <p>(A) Before 0.1.4 a full roll in the bag was a present like any other thing in it, found by the timer; now the
     * photo coming back and filling the roll is what puts the present in mind, so the case has the photo come back.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WALK_TICKS)
    public static void a_roll_of_film_that_fills_is_given_to_the_owner(GameTestHelper helper) {
        ServerPlayer owner = ownerAcross(helper);
        AbstractPet pet = ownedBy(helper, InitEntity.HACHIWARE_PET.get(), owner);
        pet.getBackpack().setItem(2, new ItemStack(InitItems.CAMERA.get()));
        pet.getBackpack().setItem(3, FilmRolls.begin(InitItems.FILM.get(), 2, shot()));

        PetMind.post(pet, new Message.PhotoResult.Stored(UUID.randomUUID(), PhotoSubject.SCENERY, "", Optional.empty(),
            BuiltInRegistries.ITEM.getKey(InitItems.CAMERA.get())));

        helper.succeedWhen(() -> {
            ItemStack given = owner.getInventory().items.stream().filter(stack -> stack.is(InitItems.FILM.get()))
                .findFirst().orElse(ItemStack.EMPTY);
            helper.assertFalse(given.isEmpty(), "the owner never got the roll");
            helper.assertTrue(FilmItem.rollOf(given).full(), "the owner was given a roll that was not full");
            helper.assertTrue(carries(pet, InitItems.CAMERA.get()), "the camera went with it");
        });
    }

    /** A roll that is not full when the next photo comes back is no occasion, and a pet has nothing to give for it. */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = NOTHING_TICKS + 20)
    public static void a_roll_that_has_room_left_is_no_occasion(GameTestHelper helper) {
        ServerPlayer owner = ownerAcross(helper);
        AbstractPet pet = ownedBy(helper, InitEntity.HACHIWARE_PET.get(), owner);
        pet.getBackpack().setItem(2, new ItemStack(InitItems.CAMERA.get()));
        pet.getBackpack().setItem(3, FilmRolls.begin(InitItems.FILM.get(), FilmRoll.MOST_EXPOSURES, shot()));

        PetMind.post(pet, new Message.PhotoResult.Stored(UUID.randomUUID(), PhotoSubject.SCENERY, "", Optional.empty(),
            BuiltInRegistries.ITEM.getKey(InitItems.CAMERA.get())));

        helper.runAfterDelay(NOTHING_TICKS, () -> {
            helper.assertTrue(pet.giftIntentions().held().isEmpty(), "the pet thought of a present for a roll it has not finished");
            helper.assertFalse(owner.getInventory().contains(new ItemStack(InitItems.FILM.get())), "the owner was given the roll");
            helper.succeed();
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
     *
     * <p>(A) The pet has a present in mind instead of a bag with something in it, see
     * {@link #a_pet_treated_with_a_cookie_gives_one_of_its_cookies_back}.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = AWAY_TICKS + WALK_TICKS)
    public static void a_free_pet_gives_its_present_once_its_owner_is_home(GameTestHelper helper) {
        ServerPlayer owner = ownerAcross(helper);
        teleportAway(helper, owner);
        AbstractPet pet = ownedBy(helper, InitEntity.USAGI_PET.get(), owner);
        pet.getBackpack().setItem(2, new ItemStack(Items.COOKIE, 2));
        thinkOf(pet, GiftOccasion.WAS_TREATED);
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
     *
     * <p>(A) The pet has a present in mind, see {@link #a_pet_treated_with_a_cookie_gives_one_of_its_cookies_back}.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WALK_TICKS)
    public static void a_following_pet_still_gives_its_present_to_an_owner_far_away(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        BlockPos corner = helper.absolutePos(new BlockPos(14, STAND, 14));
        owner.teleportTo(corner.getX() + 0.5, corner.getY(), corner.getZ() + 0.5);
        AbstractPet pet = ownedBy(helper, InitEntity.USAGI_PET.get(), owner);
        pet.setPetDirective(PetDirective.FOLLOW);
        pet.getBackpack().setItem(2, new ItemStack(Items.COOKIE, 2));
        thinkOf(pet, GiftOccasion.WAS_TREATED);

        helper.succeedWhen(() -> helper.assertTrue(owner.getInventory().countItem(Items.COOKIE) == 1,
            "the owner was not given a cookie"));
    }

    /**
     * An owner who leaves the pet's home on the way ends the errand: what the pet was carrying
     * goes back in its bag, and it does not set off again while the owner is away, nor when they are back: it had the
     * present in mind and took it out of the bag, and has it in mind no longer.
     *
     * <p>(A) The pet has a present in mind, see {@link #a_pet_treated_with_a_cookie_gives_one_of_its_cookies_back}; and
     * the pet does not think of the present again once the owner is back.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WALK_TICKS)
    public static void a_present_is_put_back_when_the_owner_leaves_home(GameTestHelper helper) {
        ServerPlayer owner = ownerAcross(helper);
        AbstractPet pet = ownedBy(helper, InitEntity.USAGI_PET.get(), owner);
        pet.getBackpack().setItem(2, new ItemStack(Items.COOKIE, 2));
        thinkOf(pet, GiftOccasion.WAS_TREATED);
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
     *
     * <p>(A) The pet has a present in mind, see {@link #a_pet_treated_with_a_cookie_gives_one_of_its_cookies_back}.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WALK_TICKS)
    public static void a_present_is_put_back_when_the_walk_is_abandoned(GameTestHelper helper) {
        ServerPlayer owner = ownerAcross(helper);
        AbstractPet pet = ownedBy(helper, InitEntity.USAGI_PET.get(), owner);
        pet.getBackpack().setItem(2, new ItemStack(Items.COOKIE, 2));
        thinkOf(pet, GiftOccasion.WAS_TREATED);
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
     * hands over one and thinks of no other, however long it is left and whatever else happens to it. The gate is the
     * pet's own: a game day after it gave a present, whatever happens to it in the meantime.
     *
     * <p>(A) Before 0.1.4 the case showed the errand did not start again in the tick after the hand-over, before the pet
     * had chosen what to do anew; now it also shows the pet does not think of another present while it rests.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = NOTHING_TICKS)
    public static void a_pet_gives_one_present_and_then_rests(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        BlockPos beside = helper.absolutePos(new BlockPos(4, STAND, 3));
        owner.teleportTo(beside.getX() + 0.5, beside.getY(), beside.getZ() + 0.5);
        AbstractPet pet = ownedBy(helper, InitEntity.USAGI_PET.get(), owner);
        pet.getBackpack().setItem(2, new ItemStack(Items.COOKIE, 5));
        thinkOf(pet, GiftOccasion.WAS_TREATED);

        helper.runAfterDelay(THINKING_TICKS, () -> {
            helper.assertTrue(owner.getInventory().countItem(Items.COOKIE) == 1,
                "the owner has " + owner.getInventory().countItem(Items.COOKIE) + " cookies, not the one");
            thinkOf(pet, GiftOccasion.WAS_TREATED);
            thinkOf(pet, GiftOccasion.FILM_FULL);
            helper.assertTrue(pet.giftIntentions().held().isEmpty(), "the pet thought of another present while it rests");
        });
        helper.runAfterDelay(THINKING_TICKS * 2, () -> {
            helper.assertTrue(owner.getInventory().countItem(Items.COOKIE) == 1,
                "the owner has " + owner.getInventory().countItem(Items.COOKIE) + " cookies, not the one");
            helper.assertTrue(count(pet, Items.COOKIE) == 4, "the pet has " + count(pet, Items.COOKIE) + " cookies left, not four");
            helper.assertTrue(pet.getPendingGift().isEmpty(), "the pet is carrying another present");
            helper.succeed();
        });
    }

    /**
     * A farmer is forever planting, and a seed is what it has most of: it is the one thing it never gives, though the list
     * of gifts has seeds on it, and a pet that works at nothing gives them. What else it has it gives.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WALK_TICKS)
    public static void a_farmer_never_gives_seeds(GameTestHelper helper) {
        ServerPlayer farmersOwner = ownerAcross(helper);
        ServerPlayer idlersOwner = ownerAcross(helper);
        AbstractPet farmer = ownedBy(helper, InitEntity.SHISA_PET.get(), farmersOwner);
        farmer.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WOODEN_HOE));
        farmer.getBackpack().setItem(2, new ItemStack(Items.WHEAT_SEEDS, 4));
        farmer.getBackpack().setItem(3, new ItemStack(Items.POPPY));
        AbstractPet idler = ownedBy(helper, InitEntity.USAGI_PET.get(), idlersOwner);
        idler.getBackpack().setItem(2, new ItemStack(Items.WHEAT_SEEDS, 4));
        thinkOf(farmer, GiftOccasion.GOT_PAID);
        thinkOf(idler, GiftOccasion.GOT_PAID);

        helper.succeedWhen(() -> {
            helper.assertTrue(idlersOwner.getInventory().countItem(Items.WHEAT_SEEDS) == 1, "the pet with no work never gave its seed");
            helper.assertTrue(farmersOwner.getInventory().countItem(Items.POPPY) == 1, "the farmer never gave the flower it had");
            helper.assertTrue(farmersOwner.getInventory().countItem(Items.WHEAT_SEEDS) == 0
                && count(farmer, Items.WHEAT_SEEDS) == 4, "the farmer gave the seeds it plants");
        });
    }

    /** A fencer's sword and an archer's arrows are what they fight with, and a pet that fights with neither gives them. */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WALK_TICKS)
    public static void a_fighter_never_gives_what_it_fights_with(GameTestHelper helper) {
        ServerPlayer fencersOwner = ownerAcross(helper);
        ServerPlayer archersOwner = ownerAcross(helper);
        ServerPlayer idlersOwner = ownerAcross(helper);
        AbstractPet fencer = ownedBy(helper, InitEntity.SHISA_PET.get(), fencersOwner);
        fencer.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WOODEN_SWORD));
        fencer.getBackpack().setItem(2, new ItemStack(Items.IRON_SWORD));
        fencer.getBackpack().setItem(3, new ItemStack(Items.POPPY));
        AbstractPet archer = ownedBy(helper, InitEntity.USAGI_PET.get(), archersOwner);
        archer.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        archer.getBackpack().setItem(2, new ItemStack(Items.ARROW, 4));
        archer.getBackpack().setItem(3, new ItemStack(Items.POPPY));
        AbstractPet idler = ownedBy(helper, InitEntity.HACHIWARE_PET.get(), idlersOwner);
        idler.getBackpack().setItem(2, new ItemStack(Items.IRON_SWORD));
        idler.getBackpack().setItem(3, new ItemStack(Items.ARROW, 4));
        List.of(fencer, archer, idler).forEach(pet -> thinkOf(pet, GiftOccasion.GOT_PAID));

        helper.succeedWhen(() -> {
            helper.assertTrue(idlersOwner.getInventory().countItem(Items.IRON_SWORD) + idlersOwner.getInventory().countItem(Items.ARROW) == 1,
                "the pet with no work never gave one of its sword and its arrows");
            helper.assertTrue(fencersOwner.getInventory().countItem(Items.POPPY) == 1, "the fencer never gave the flower it had");
            helper.assertTrue(archersOwner.getInventory().countItem(Items.POPPY) == 1, "the archer never gave the flower it had");
            helper.assertTrue(fencersOwner.getInventory().countItem(Items.IRON_SWORD) == 0 && carries(fencer, Items.IRON_SWORD),
                "the fencer gave a sword");
            helper.assertTrue(archersOwner.getInventory().countItem(Items.ARROW) == 0 && count(archer, Items.ARROW) == 4,
                "the archer gave its arrows");
        });
    }

    /** Four pets of one owner each have a present for them and the owner is given three, whichever pets they come from. */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WALK_TICKS)
    public static void an_owner_gets_at_most_three_presents_a_day_from_all_their_pets(GameTestHelper helper) {
        ServerPlayer owner = ownerAcross(helper);
        List<AbstractPet> pets = new ArrayList<>();
        for (int pet = 0; pet < PETS; pet++) {
            AbstractPet each = GameTestKit.wild(helper, InitEntity.USAGI_PET.get(), new BlockPos(3 + pet, STAND, 3));
            each.tame(owner);
            each.setPetDirective(PetDirective.FREE);
            each.getBackpack().setItem(2, new ItemStack(Items.POPPY));
            thinkOf(each, GiftOccasion.GOT_PAID);
            pets.add(each);
        }

        helper.runAfterDelay(NOTHING_TICKS, () -> {
            helper.assertTrue(owner.getInventory().countItem(Items.POPPY) == DAILY_LIMIT,
                "the owner has " + owner.getInventory().countItem(Items.POPPY) + " flowers, not " + DAILY_LIMIT);
            long giving = pets.stream().filter(each -> each.getBrain().hasMemoryValue(InitMemory.GIFT_COOLDOWN.get())
                && !carries(each, Items.POPPY)).count();
            helper.assertTrue(giving == DAILY_LIMIT, giving + " pets gave a present");
            helper.assertTrue(pets.stream().allMatch(each -> each.getPendingGift().isEmpty()), "a pet is still carrying its present");
            ServerLevel level = helper.getLevel();
            helper.assertTrue(GiftLedger.of(level).leftToday(level, owner.getUUID()) == 0, "the owner may be given more today");
            helper.succeed();
        });
    }

    /**
     * Taken with a click, the present is in the owner's hands at once, wherever the pet is on its way: the pet rests as
     * for a present it gave, the owner counts it, and the pet that has nothing left to bring ends what it was doing and
     * does not bring another.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WALK_TICKS)
    public static void a_present_is_taken_with_a_right_click(GameTestHelper helper) {
        ServerPlayer owner = ownerAcross(helper);
        owner.setGameMode(GameType.SURVIVAL);
        AbstractPet pet = ownedBy(helper, InitEntity.USAGI_PET.get(), owner);
        pet.getBackpack().setItem(2, new ItemStack(Items.COOKIE, 3));
        thinkOf(pet, GiftOccasion.WAS_TREATED);
        owner.setShiftKeyDown(false);

        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(pet.getIntent().filter(ErrandIds.GIFT_OWNER::equals).isPresent()
                && !pet.getPendingGift().isEmpty(), "the pet never set off with a present"))
            .thenExecute(() -> {
                // A hand that is free: the handbook the advancement gave the owner on joining is in it otherwise.
                owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                helper.assertTrue(pet.mobInteract(owner, InteractionHand.MAIN_HAND).consumesAction(), "the click was not taken");
                helper.assertTrue(owner.getInventory().countItem(Items.COOKIE) == 1, "the owner was not given the cookie at once");
                helper.assertTrue(pet.getPendingGift().isEmpty(), "the pet still holds the present");
                ServerLevel level = helper.getLevel();
                helper.assertTrue(GiftLedger.of(level).leftToday(level, owner.getUUID()) == DAILY_LIMIT - 1,
                    "the present was not counted against the owner's day");
                helper.assertTrue(pet.getBrain().getTimeUntilExpiry(InitMemory.GIFT_COOLDOWN.get()) > RESTING,
                    "the pet is not resting from having given a present");
            })
            .thenExecuteAfter(SETTLED_TICKS, () -> {
                helper.assertTrue(pet.getIntent().filter(ErrandIds.GIFT_OWNER::equals).isEmpty(),
                    "the pet goes on bringing a present that is no longer in its paws");
                helper.assertTrue(owner.getInventory().countItem(Items.COOKIE) == 1, "the owner was given a second cookie");
                helper.assertTrue(count(pet, Items.COOKIE) == 2, "the pet has " + count(pet, Items.COOKIE) + " cookies, not the two");
            })
            .thenSucceed();
    }

    /**
     * A pet that has been told its owner is away for good thinks of the present no longer: the thought fades, and an owner
     * who comes home a day later than it was thought of is given nothing; the next thing that happens puts another in mind.
     */
    @GameTest(template = "floor16", batch = FADE_BATCH, timeoutTicks = AWAY_TICKS + WALK_TICKS)
    public static void an_intention_fades_when_the_owner_stays_away(GameTestHelper helper) {
        ServerPlayer owner = ownerAcross(helper);
        teleportAway(helper, owner);
        AbstractPet pet = ownedBy(helper, InitEntity.USAGI_PET.get(), owner);
        pet.getBackpack().setItem(2, new ItemStack(Items.COOKIE, 2));
        thinkOf(pet, GiftOccasion.WAS_TREATED);

        helper.startSequence()
            .thenExecuteAfter(FADE_TICKS + SETTLED_TICKS, () -> {
                BlockPos near = helper.absolutePos(new BlockPos(4, STAND, 3));
                owner.teleportTo(near.getX() + 0.5, near.getY(), near.getZ() + 0.5);
            })
            .thenExecuteAfter(AWAY_TICKS, () -> {
                helper.assertTrue(owner.getInventory().countItem(Items.COOKIE) == 0, "the owner was given a present that was forgotten");
                helper.assertTrue(count(pet, Items.COOKIE) == 2, "the pet gave away what it was no longer thinking of giving");
                thinkOf(pet, GiftOccasion.WAS_TREATED);
                helper.assertTrue(pet.giftIntentions().held().filter(held -> !held.hasFaded(helper.getLevel().getGameTime())).isPresent(),
                    "the pet did not think of another present once the first was forgotten");
            })
            .thenWaitUntil(() -> helper.assertTrue(owner.getInventory().countItem(Items.COOKIE) == 1, "the owner was not given the second"))
            .thenSucceed();
    }

    /**
     * An owner asleep in a bed is given their present as a cat gives its morning gift: the pet waits by the bed, lying
     * quiet, and when the owner wakes in the morning leaves it on the floor beside them.
     */
    @GameTest(template = "floor16", batch = BED_BATCH, timeoutTicks = WALK_TICKS)
    public static void a_sleeping_owner_finds_a_present_by_the_bed_in_the_morning(GameTestHelper helper) {
        BlockPos head = new BlockPos(10, STAND, 10);
        ServerPlayer owner = sleeper(helper, head);
        AbstractPet pet = ownedBy(helper, InitEntity.USAGI_PET.get(), owner);
        pet.getBackpack().setItem(2, new ItemStack(Items.COOKIE, 3));
        thinkOf(pet, GiftOccasion.WAS_TREATED);
        ServerLevel level = helper.getLevel();

        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(pet.getActivity() == PetActivity.SLEEP && !pet.getPendingGift().isEmpty(),
                "the pet never lay down by the bed with the present"))
            .thenExecute(() -> {
                helper.assertTrue(owner.getInventory().countItem(Items.COOKIE) == 0 && cookiesNear(helper, head).isEmpty(),
                    "the present was given before the owner woke");
                level.setDayTime(DAWN);
                owner.stopSleepInBed(false, true);
            })
            .thenWaitUntil(() -> helper.assertTrue(!cookiesNear(helper, head).isEmpty(), "no present was left by the bed"))
            .thenExecute(() -> {
                helper.assertTrue(owner.getInventory().countItem(Items.COOKIE) == 0, "the present was put into the owner's hands");
                helper.assertTrue(pet.getPendingGift().isEmpty() && count(pet, Items.COOKIE) == 2, "the pet is still holding the present");
                helper.assertTrue(pet.getBrain().getTimeUntilExpiry(InitMemory.GIFT_COOLDOWN.get()) > RESTING,
                    "the pet is not resting from having given a present");
                helper.assertTrue(GiftLedger.of(level).leftToday(level, owner.getUUID()) == DAILY_LIMIT - 1,
                    "the present was not counted against the owner's day");
                helper.assertTrue(pet.getActivity() == PetActivity.NONE, "the pet is still lying by the bed");
            })
            .thenSucceed();
    }

    /**
     * An owner woken in the middle of the night wakes to no present: a cat leaves its gift only in the morning. The pet
     * gives up and puts it back.
     */
    @GameTest(template = "floor16", batch = NIGHT_BATCH, timeoutTicks = WALK_TICKS)
    public static void an_owner_woken_in_the_night_is_given_nothing_by_the_bed(GameTestHelper helper) {
        BlockPos head = new BlockPos(10, STAND, 10);
        ServerPlayer owner = sleeper(helper, head);
        AbstractPet pet = ownedBy(helper, InitEntity.USAGI_PET.get(), owner);
        pet.getBackpack().setItem(2, new ItemStack(Items.COOKIE, 3));
        thinkOf(pet, GiftOccasion.WAS_TREATED);

        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(pet.getActivity() == PetActivity.SLEEP && !pet.getPendingGift().isEmpty(),
                "the pet never lay down by the bed with the present"))
            .thenExecute(() -> owner.stopSleepInBed(false, true))
            .thenWaitUntil(() -> helper.assertTrue(pet.getPendingGift().isEmpty() && pet.getActivity() == PetActivity.NONE,
                "the pet is still waiting by the bed"))
            .thenExecuteAfter(SETTLED_TICKS, () -> {
                helper.assertTrue(cookiesNear(helper, head).isEmpty() && owner.getInventory().countItem(Items.COOKIE) == 0,
                    "a present was given to an owner woken in the night");
                helper.assertTrue(count(pet, Items.COOKIE) == 3, "the present was not put back: " + count(pet, Items.COOKIE) + " cookies");
                helper.assertTrue(pet.getBrain().getTimeUntilExpiry(InitMemory.GIFT_COOLDOWN.get()) < RESTING,
                    "a present never given tired the pet for a day");
            })
            .thenSucceed();
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

    /** An owner asleep in a bed whose head is here, and the bed. */
    private static ServerPlayer sleeper(GameTestHelper helper, BlockPos head) {
        BlockState bed = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.EAST);
        helper.setBlock(head.west(), bed.setValue(BedBlock.PART, BedPart.FOOT));
        helper.setBlock(head, bed.setValue(BedBlock.PART, BedPart.HEAD));
        ServerPlayer owner = player(helper);
        BlockPos in = helper.absolutePos(head);
        owner.teleportTo(in.getX() + 0.5, in.getY() + 0.6, in.getZ() + 0.5);
        owner.startSleeping(in);
        return owner;
    }

    /** The cookies lying on the floor near the bed. */
    private static List<ItemEntity> cookiesNear(GameTestHelper helper, BlockPos head) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(head)).inflate(4.0),
            item -> item.getItem().is(Items.COOKIE));
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
