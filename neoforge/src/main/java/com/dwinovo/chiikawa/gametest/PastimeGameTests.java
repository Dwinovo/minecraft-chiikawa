package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.asTeam;
import static com.dwinovo.chiikawa.gametest.GameTestKit.assertSaid;
import static com.dwinovo.chiikawa.gametest.GameTestKit.player;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.data.CharacterPastimeData;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.Outcome;
import com.dwinovo.chiikawa.entity.brain.errand.decide.DecisionLog;
import com.dwinovo.chiikawa.entity.brain.errand.run.PetIcon;
import com.dwinovo.chiikawa.entity.brain.errand.view.OwnerSeen;
import com.dwinovo.chiikawa.entity.interact.PetInteractHandler;
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The pastimes that are one character's own (0.1.4 design, the first batch of section 11): that Chiikawa cries at a storm until
 * its owner comes, Momonga asks to be praised and sulks or is glad, Kurimanju has its drink and brings a coffee, Usagi has its
 * fits and never cries, Rakko stares at sweets and practises, Shisa bows to an owner come home and Hachiware is not put out
 * by the rain.
 *
 * <p>Each case has a batch of its own: they are about a pet that looks for a sweet, an owner or another pet, and the one in
 * the next yard along is one it would find.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PastimeGameTests {
    private static final String STORM_BATCH = "chiikawa_pastime_storm";
    private static final String SHOWER_BATCH = "chiikawa_pastime_shower";
    private static final String COPY_BATCH = "chiikawa_pastime_copy";
    private static final String SULK_BATCH = "chiikawa_pastime_sulk";
    private static final String PRAISED_BATCH = "chiikawa_pastime_praised";
    private static final String FRIEND_BATCH = "chiikawa_pastime_friend";
    private static final String DRINK_BATCH = "chiikawa_pastime_drink";
    private static final String NO_DRINK_BATCH = "chiikawa_pastime_no_drink";
    private static final String COFFEE_BATCH = "chiikawa_pastime_coffee";
    private static final String FRENZY_BATCH = "chiikawa_pastime_frenzy";
    private static final String SWEETS_BATCH = "chiikawa_pastime_sweets";
    private static final String HELD_SWEETS_BATCH = "chiikawa_pastime_held_sweets";
    private static final String STONES_BATCH = "chiikawa_pastime_stones";
    private static final String PRACTICE_BATCH = "chiikawa_pastime_practice";
    private static final String GREET_BATCH = "chiikawa_pastime_greet";
    private static final String TROUBLE_BATCH = "chiikawa_pastime_trouble";

    private static final int STAND = 2;
    private static final int WALK_TICKS = 1200;
    /** How long a pet is watched for something it should not do. */
    private static final int WATCH_TICKS = 200;
    /** How long the sky keeps raining, in ticks: longer than a case. */
    private static final int RAIN_TICKS = 24000;
    /** The hour the day is ending at, and held there while a case about the evening lasts. */
    private static final long DUSK = 12500L;
    /**
     * A spell of work long enough to be one: longer than the settings' twenty seconds by more than the two seconds between
     * one look of the pet's and the next at each end of it.
     */
    private static final int WORK_TICKS = 500;
    /** The sensor looks every two seconds, and the owner was away for longer than the settings' two minutes. */
    private static final long LONG_AGO = 3000L;

    private PastimeGameTests() {
    }

    @BeforeBatch(batch = STORM_BATCH)
    public static void aStorm(ServerLevel level) {
        storm(level);
    }

    @BeforeBatch(batch = SHOWER_BATCH)
    public static void aShower(ServerLevel level) {
        rain(level);
    }

    @BeforeBatch(batch = TROUBLE_BATCH)
    public static void rainForTrouble(ServerLevel level) {
        rain(level);
    }

    @BeforeBatch(batch = COPY_BATCH)
    public static void dayToCopyIn(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    @BeforeBatch(batch = SULK_BATCH)
    public static void dayToSulkIn(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    @BeforeBatch(batch = PRAISED_BATCH)
    public static void dayToBePraisedIn(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    @BeforeBatch(batch = FRIEND_BATCH)
    public static void dayToBePraisedByAFriendIn(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    @BeforeBatch(batch = DRINK_BATCH)
    public static void duskForADrink(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, DUSK);
    }

    @BeforeBatch(batch = NO_DRINK_BATCH)
    public static void dayForNoDrink(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    @BeforeBatch(batch = COFFEE_BATCH)
    public static void dayForCoffee(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    @BeforeBatch(batch = FRENZY_BATCH)
    public static void dayForAFit(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    @BeforeBatch(batch = SWEETS_BATCH)
    public static void dayForSweets(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    @BeforeBatch(batch = HELD_SWEETS_BATCH)
    public static void dayForSweetsInAHand(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    @BeforeBatch(batch = STONES_BATCH)
    public static void dayForStones(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    @BeforeBatch(batch = PRACTICE_BATCH)
    public static void dayToPractise(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    @BeforeBatch(batch = GREET_BATCH)
    public static void dayToGreet(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    @AfterBatch(batch = STORM_BATCH)
    public static void clearAfterTheStorm(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    @AfterBatch(batch = SHOWER_BATCH)
    public static void dryAfterTheShower(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    @AfterBatch(batch = TROUBLE_BATCH)
    public static void dryAfterTheTrouble(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    @AfterBatch(batch = DRINK_BATCH)
    public static void dayAfterTheDrink(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    /** It storms for as long as a case lasts, by day. */
    private static void storm(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
        level.setWeatherParameters(0, RAIN_TICKS, true, true);
    }

    /** It rains for as long as a case lasts, by day. */
    private static void rain(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
        level.setWeatherParameters(0, RAIN_TICKS, true, false);
    }

    /**
     * Chiikawa out in a storm, with its owner a long way off: it cries and cries as long as nobody is by it, and the moment
     * its owner is, it stops. It is not made to run off nor hide, which a pet that is hurt is, and it does not take the shelter
     * a pet takes in the rain, which a storm is.
     */
    @GameTest(template = "floor16", batch = STORM_BATCH, timeoutTicks = WALK_TICKS)
    public static void chiikawa_cowers_at_thunder_until_its_owner_comes(GameTestHelper helper) {
        GameTestKit.openSky(helper);
        ServerPlayer owner = ownerAt(helper, new BlockPos(14, STAND, 14));
        AbstractPet pet = ownedBy(helper, InitEntity.CHIIKAWA_PET.get(), owner, new BlockPos(4, STAND, 4));
        // A word it happens to say on the way would keep it quiet when it is frightened, as a pet keeps quiet after speaking.
        helper.onEachTick(() -> {
            if (!pet.getMind().errands().isOn(CharacterPastimeData.COWER)) {
                pet.getBrain().eraseMemory(InitMemory.LAST_SAID.get());
            }
        });

        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(pet.getMind().errands().isOn(CharacterPastimeData.COWER)
                    && pet.getPerformance().startsWith("cry"),
                "the pet never cowered at the storm: it is " + pet.getMind().errands().errand() + " " + pet.getPerformance()))
            .thenExecute(() -> assertSaid(helper, pet, VoiceMoment.SCARED))
            .thenExecuteFor(WATCH_TICKS, () -> helper.assertTrue(pet.getMind().errands().isOn(CharacterPastimeData.COWER),
                "the pet stopped crying with nobody by it"))
            .thenExecute(() -> {
                BlockPos beside = pet.blockPosition().east();
                owner.teleportTo(beside.getX() + 0.5, beside.getY(), beside.getZ() + 0.5);
            })
            .thenWaitUntil(() -> helper.assertTrue(!pet.getMind().errands().isOn(CharacterPastimeData.COWER)
                    && pet.getPerformance().isEmpty(),
                "the pet went on crying with its owner beside it: " + pet.getMind().errands().errand()))
            .thenSucceed();
    }

    /** A shower is not a storm: Chiikawa does not cry at it. */
    @GameTest(template = "floor16", batch = SHOWER_BATCH, timeoutTicks = WATCH_TICKS + 20)
    public static void chiikawa_does_not_cower_in_a_shower(GameTestHelper helper) {
        GameTestKit.openSky(helper);
        ServerPlayer owner = ownerAt(helper, new BlockPos(14, STAND, 14));
        AbstractPet pet = ownedBy(helper, InitEntity.CHIIKAWA_PET.get(), owner, new BlockPos(4, STAND, 4));

        helper.startSequence()
            .thenExecuteFor(WATCH_TICKS, () -> helper.assertFalse(pet.getMind().errands().isOn(CharacterPastimeData.COWER),
                "the pet cowered at a shower"))
            .thenSucceed();
    }

    /**
     * Hachiware in the rain with its owner by: it scratches its head and says something will turn up, and goes on with what it
     * was doing.
     */
    @GameTest(template = "floor16", batch = TROUBLE_BATCH, timeoutTicks = WALK_TICKS)
    public static void hachiware_says_something_will_turn_up_when_it_rains(GameTestHelper helper) {
        GameTestKit.quietYard(helper, NOON);
        helper.getLevel().setWeatherParameters(0, RAIN_TICKS, true, false);
        ServerPlayer owner = ownerAt(helper, new BlockPos(6, STAND, 5));
        AbstractPet pet = ownedBy(helper, InitEntity.HACHIWARE_PET.get(), owner, new BlockPos(4, STAND, 4));

        helper.startSequence()
            .thenWaitUntil(() -> assertSaid(helper, pet, VoiceMoment.TROUBLE))
            .thenSucceed();
    }

    /**
     * Momonga with another pet nearby that is in the middle of a scene: it copies it, for want of anything better to be cute
     * with, and then goes to its owner and asks, with the icon over its head that says what for.
     */
    @GameTest(template = "floor16", batch = COPY_BATCH, timeoutTicks = WALK_TICKS)
    public static void momonga_copies_the_pet_beside_it_before_it_asks_its_owner_for_praise(GameTestHelper helper) {
        ServerPlayer owner = ownerAt(helper, new BlockPos(12, STAND, 10));
        AbstractPet neighbour = sittingBeside(helper, owner);
        neighbour.setPerformance("clap");
        AbstractPet pet = ownedBy(helper, InitEntity.MOMONGA_PET.get(), owner, new BlockPos(4, STAND, 4));

        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(pet.getMind().errands().isOn(CharacterPastimeData.ASK_PRAISE)
                    && pet.getPerformance().equals("clap,cute"),
                "the pet never copied its neighbour: it is " + pet.getMind().errands().errand() + " " + pet.getPerformance()))
            .thenWaitUntil(() -> helper.assertTrue(pet.getIcon() == PetIcon.PRAISE && pet.distanceTo(owner) < 4.0
                    && pet.getPerformance().equals("ask_praise"),
                "the pet never came to its owner to ask: it is " + pet.getMind().errands().errand() + " " + pet.getPerformance()))
            .thenSucceed();
    }

    /**
     * Nobody praises Momonga: it asks for as long as it waits, and then it has a tantrum and says it is put out. It is done
     * with it after that, and the icon is gone.
     */
    @GameTest(template = "floor16", batch = SULK_BATCH, timeoutTicks = WALK_TICKS + 600)
    public static void momonga_sulks_when_nobody_praises_it(GameTestHelper helper) {
        ServerPlayer owner = ownerAt(helper, new BlockPos(12, STAND, 10));
        sittingBeside(helper, owner);
        AbstractPet pet = ownedBy(helper, InitEntity.MOMONGA_PET.get(), owner, new BlockPos(4, STAND, 4));

        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(pet.getIcon() == PetIcon.PRAISE, "the pet never asked to be praised"))
            .thenWaitUntil(() -> helper.assertTrue(!pet.getMind().errands().isOn(CharacterPastimeData.ASK_PRAISE),
                "the pet asked for ever"))
            .thenExecute(() -> {
                assertSaid(helper, pet, VoiceMoment.TURNED_DOWN);
                helper.assertTrue(pet.getIcon() != PetIcon.PRAISE, "the pet still wears the icon of one who wants praising");
            })
            .thenSucceed();
    }

    /**
     * A click from the owner is the praise: Momonga is glad of it and done at once, long before it would have given up, and
     * says nothing of being turned down.
     */
    @GameTest(template = "floor16", batch = PRAISED_BATCH, timeoutTicks = WALK_TICKS)
    public static void momonga_is_happy_when_praised(GameTestHelper helper) {
        ServerPlayer owner = ownerAt(helper, new BlockPos(12, STAND, 10));
        sittingBeside(helper, owner);
        AbstractPet pet = ownedBy(helper, InitEntity.MOMONGA_PET.get(), owner, new BlockPos(4, STAND, 4));
        long[] praisedAt = {-1};

        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(pet.getIcon() == PetIcon.PRAISE, "the pet never asked to be praised"))
            .thenExecute(() -> {
                owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                InteractionResult result = PetInteractHandler.handle(pet, owner, InteractionHand.MAIN_HAND);
                helper.assertTrue(result.consumesAction(), "the click was not taken: " + result);
                praisedAt[0] = helper.getLevel().getGameTime();
            })
            .thenWaitUntil(() -> helper.assertTrue(!pet.getMind().errands().isOn(CharacterPastimeData.ASK_PRAISE)
                    && pet.getIcon() != PetIcon.PRAISE,
                "the pet went on asking after it was praised"))
            .thenExecute(() -> {
                helper.assertTrue(helper.getLevel().getGameTime() - praisedAt[0] < 40, "the pet was a long while being glad of it");
                helper.assertTrue(pet.getMind().log().entries().stream().anyMatch(entry ->
                        entry instanceof DecisionLog.Ended ended && ended.errand().equals(CharacterPastimeData.ASK_PRAISE)
                            && ended.outcome() instanceof Outcome.Done),
                    "the praise was not the end of it: " + pet.getMind().log().entries());
                helper.assertFalse(GameTestKit.said(pet).filter(line -> GameTestKit.isLine(pet, VoiceMoment.TURNED_DOWN, line)).isPresent(),
                    "the pet said it was turned down after it was praised");
            })
            .thenSucceed();
    }

    /** A friend of the owner praises as the owner does; somebody who is nobody's friend does not. */
    @GameTest(template = "floor16", batch = FRIEND_BATCH, timeoutTicks = WALK_TICKS)
    public static void a_friend_may_praise_a_pet_that_asks_and_a_stranger_may_not(GameTestHelper helper) {
        ServerPlayer owner = ownerAt(helper, new BlockPos(12, STAND, 10));
        ServerPlayer friend = ownerAt(helper, new BlockPos(13, STAND, 10));
        ServerPlayer stranger = ownerAt(helper, new BlockPos(11, STAND, 10));
        sittingBeside(helper, owner);
        AbstractPet pet = ownedBy(helper, InitEntity.MOMONGA_PET.get(), owner, new BlockPos(4, STAND, 4));

        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(pet.getIcon() == PetIcon.PRAISE, "the pet never asked to be praised"))
            .thenExecute(() -> {
                stranger.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                PetInteractHandler.handle(pet, stranger, InteractionHand.MAIN_HAND);
            })
            .thenExecuteFor(20, () -> helper.assertTrue(pet.getIcon() == PetIcon.PRAISE, "a stranger praised somebody else's pet"))
            .thenExecute(() -> asTeam(helper, () -> {
                friend.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                PetInteractHandler.handle(pet, friend, InteractionHand.MAIN_HAND);
            }, owner, friend))
            .thenWaitUntil(() -> helper.assertTrue(pet.getIcon() != PetIcon.PRAISE && !pet.getMind().errands().isOn(CharacterPastimeData.ASK_PRAISE),
                "a friend's praise was not taken"))
            .thenSucceed();
    }

    /** As the day ends Kurimanju sits down with a drink, and lets out a breath when it is done. */
    @GameTest(template = "floor16", batch = DRINK_BATCH, timeoutTicks = WALK_TICKS)
    public static void kurimanju_drinks_in_the_evening(GameTestHelper helper) {
        GameTestKit.quietYard(helper, DUSK);
        helper.onEachTick(() -> helper.getLevel().setDayTime(DUSK));
        AbstractPet pet = GameTestKit.owned(helper, InitEntity.KURIMANJU_PET.get(), new BlockPos(4, STAND, 4));

        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(pet.getMind().errands().isOn(CharacterPastimeData.EVENING_DRINK)
                    && pet.getPerformance().startsWith("drink"),
                "the pet never sat down with a drink: it is " + pet.getMind().errands().errand() + " " + pet.getPerformance()))
            .thenWaitUntil(() -> helper.assertTrue(!pet.getMind().errands().isOn(CharacterPastimeData.EVENING_DRINK),
                "the pet never finished its drink"))
            .thenExecute(() -> assertSaid(helper, pet, VoiceMoment.SIGH))
            .thenSucceed();
    }

    /** By day Kurimanju has no drink. */
    @GameTest(template = "floor16", batch = NO_DRINK_BATCH, timeoutTicks = WATCH_TICKS + 20)
    public static void kurimanju_does_not_drink_by_day(GameTestHelper helper) {
        AbstractPet pet = GameTestKit.owned(helper, InitEntity.KURIMANJU_PET.get(), new BlockPos(4, STAND, 4));

        helper.startSequence()
            .thenExecuteFor(WATCH_TICKS, () -> helper.assertFalse(pet.getMind().errands().isOn(CharacterPastimeData.EVENING_DRINK),
                "the pet had a drink by day"))
            .thenSucceed();
    }

    /**
     * Kurimanju sees its owner swing a pickaxe for a long time and then leave off: it comes over, hands them a coffee that
     * is not there to hand, and says something kind.
     */
    @GameTest(template = "floor16", batch = COFFEE_BATCH, timeoutTicks = WALK_TICKS + WORK_TICKS)
    public static void kurimanju_brings_its_owner_a_coffee_after_a_long_job(GameTestHelper helper) {
        ServerPlayer owner = ownerAt(helper, new BlockPos(7, STAND, 5));
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
        AbstractPet pet = ownedBy(helper, InitEntity.KURIMANJU_PET.get(), owner, new BlockPos(4, STAND, 4));

        helper.startSequence()
            .thenExecuteFor(WORK_TICKS, () -> owner.swing(InteractionHand.MAIN_HAND))
            .thenExecute(() -> {
                // The owner is a stand-in that is never ticked, so it does not stop swinging by itself.
                owner.swinging = false;
                owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            })
            .thenWaitUntil(() -> helper.assertTrue(pet.getMind().errands().isOn(CharacterPastimeData.COFFEE_FOR_A_WORKER)
                    && pet.getPerformance().startsWith("hand_over"),
                "the pet never handed its owner a coffee: it is " + pet.getMind().errands().errand() + " " + pet.getPerformance()))
            .thenExecute(() -> assertSaid(helper, pet, VoiceMoment.OFFER_COFFEE))
            .thenSucceed();
    }

    /** Usagi has a fit with nothing to set it off: it yells, scampers, never cries, and does not do it again for a long while. */
    @GameTest(template = "floor16", batch = FRENZY_BATCH, timeoutTicks = WALK_TICKS)
    public static void usagi_runs_wild_and_never_cries(GameTestHelper helper) {
        AbstractPet pet = GameTestKit.owned(helper, InitEntity.USAGI_PET.get(), new BlockPos(4, STAND, 4));
        // A stroll first would have it chatter as it goes, and a pet that has just spoken lets its next line pass unheard.
        GameTestKit.putOff(pet, ErrandIds.WANDER);
        GameTestKit.wake(pet, CharacterPastimeData.FRENZY);
        boolean[] cried = {false};
        helper.onEachTick(() -> cried[0] |= pet.getPerformance().startsWith("cry") || pet.getIcon() == PetIcon.SCARED);

        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(pet.getMind().errands().isOn(CharacterPastimeData.FRENZY)
                    && pet.getPerformance().startsWith("frenzy"),
                "the pet never had its fit: it is " + pet.getMind().errands().errand() + " " + pet.getPerformance()))
            .thenExecute(() -> assertSaid(helper, pet, VoiceMoment.FRENZY))
            .thenWaitUntil(() -> helper.assertTrue(!pet.getMind().errands().isOn(CharacterPastimeData.FRENZY), "the fit never ended"))
            .thenExecuteFor(WATCH_TICKS, () -> helper.assertFalse(pet.getMind().errands().isOn(CharacterPastimeData.FRENZY),
                "the pet had another fit at once"))
            .thenExecute(() -> helper.assertFalse(cried[0], "Usagi cried"))
            .thenSucceed();
    }

    /** A cake lies a few blocks off: Rakko goes over and stares at it, and says so. */
    @GameTest(template = "floor16", batch = SWEETS_BATCH, timeoutTicks = WALK_TICKS)
    public static void rakko_stares_at_sweets(GameTestHelper helper) {
        GameTestKit.quietYard(helper, NOON);
        BlockPos cake = new BlockPos(9, STAND, 8);
        helper.spawnItem(Items.CAKE, cake);
        AbstractPet pet = GameTestKit.owned(helper, InitEntity.RAKKO_PET.get(), new BlockPos(4, STAND, 4));

        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(pet.getMind().errands().isOn(CharacterPastimeData.SWEETS_STARE)
                    && pet.getPerformance().startsWith("stare") && pet.distanceToSqr(helper.absolutePos(cake).getCenter()) < 4.0,
                "the pet never came to stare at the cake: it is " + pet.getMind().errands().errand() + " " + pet.getPerformance()))
            .thenExecute(() -> assertSaid(helper, pet, VoiceMoment.SWEETS))
            .thenSucceed();
    }

    /** The sweet is in the owner's hand, and Rakko goes over to the owner for it. */
    @GameTest(template = "floor16", batch = HELD_SWEETS_BATCH, timeoutTicks = WALK_TICKS)
    public static void rakko_stares_at_sweets_in_its_owners_hand(GameTestHelper helper) {
        GameTestKit.quietYard(helper, NOON);
        ServerPlayer owner = ownerAt(helper, new BlockPos(9, STAND, 8));
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COOKIE));
        AbstractPet pet = ownedBy(helper, InitEntity.RAKKO_PET.get(), owner, new BlockPos(4, STAND, 4));

        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(pet.getMind().errands().isOn(CharacterPastimeData.SWEETS_STARE)
                    && pet.getPerformance().startsWith("stare") && pet.distanceTo(owner) < 2.5,
                "the pet never came to stare at the cookie in its owner's hand: it is " + pet.getMind().errands().errand()))
            .thenSucceed();
    }

    /** Rakko is no more drawn to a stone than to anything else that is not sweet. */
    @GameTest(template = "floor16", batch = STONES_BATCH, timeoutTicks = WATCH_TICKS + 20)
    public static void rakko_leaves_what_is_not_sweet_alone(GameTestHelper helper) {
        helper.spawnItem(Items.COBBLESTONE, new BlockPos(9, STAND, 8));
        ServerPlayer owner = ownerAt(helper, new BlockPos(8, STAND, 8));
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        AbstractPet pet = ownedBy(helper, InitEntity.RAKKO_PET.get(), owner, new BlockPos(4, STAND, 4));

        helper.startSequence()
            .thenExecuteFor(WATCH_TICKS, () -> helper.assertFalse(pet.getMind().errands().isOn(CharacterPastimeData.SWEETS_STARE),
                "the pet stared at a stone"))
            .thenSucceed();
    }

    /** Rakko, with nobody about, swings its sword for a while. */
    @GameTest(template = "floor16", batch = PRACTICE_BATCH, timeoutTicks = WALK_TICKS)
    public static void rakko_practises_with_its_sword_when_it_has_nothing_to_do(GameTestHelper helper) {
        AbstractPet pet = GameTestKit.owned(helper, InitEntity.RAKKO_PET.get(), new BlockPos(4, STAND, 4));
        GameTestKit.wake(pet, CharacterPastimeData.SWORD_PRACTICE);

        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(pet.getMind().errands().isOn(CharacterPastimeData.SWORD_PRACTICE)
                    && pet.getPerformance().startsWith("practice"),
                "the pet never practised: it is " + pet.getMind().errands().errand() + " " + pet.getPerformance()))
            .thenWaitUntil(() -> helper.assertTrue(!pet.getMind().errands().isOn(CharacterPastimeData.SWORD_PRACTICE),
                "the pet practised for ever"))
            .thenExecuteFor(WATCH_TICKS, () -> helper.assertFalse(pet.getMind().errands().isOn(CharacterPastimeData.SWORD_PRACTICE),
                "the pet practised again at once"))
            .thenSucceed();
    }

    /**
     * An owner who has been away for a long time comes back within sight of Shisa: it sees them come, goes over to them and
     * bows, with its words of welcome. The sensor is what sees them come, so the pet is told only that it last saw them long ago.
     */
    @GameTest(template = "floor16", batch = GREET_BATCH, timeoutTicks = WALK_TICKS)
    public static void shisa_bows_when_its_owner_returns(GameTestHelper helper) {
        GameTestKit.quietYard(helper, NOON);
        ServerPlayer owner = ownerAt(helper, new BlockPos(14, STAND, 14));
        AbstractPet pet = ownedBy(helper, InitEntity.SHISA_PET.get(), owner, new BlockPos(4, STAND, 4));
        long now = helper.getLevel().getGameTime();
        pet.getBrain().setMemory(InitMemory.OWNER_SEEN.get(), new OwnerSeen(now - LONG_AGO, OwnerSeen.NEVER, OwnerSeen.NEVER, OwnerSeen.NEVER));
        // A word it happens to say while it waits would keep it quiet when its owner comes, as a pet keeps quiet after speaking.
        helper.onEachTick(() -> {
            if (!pet.getMind().errands().isOn(CharacterPastimeData.GREET_OWNER)) {
                pet.getBrain().eraseMemory(InitMemory.LAST_SAID.get());
            }
        });

        helper.startSequence()
            .thenExecuteFor(WATCH_TICKS, () -> helper.assertFalse(pet.getMind().errands().isOn(CharacterPastimeData.GREET_OWNER),
                "the pet greeted an owner who had not come back"))
            .thenExecute(() -> {
                BlockPos near = helper.absolutePos(new BlockPos(8, STAND, 5));
                owner.teleportTo(near.getX() + 0.5, near.getY(), near.getZ() + 0.5);
            })
            .thenWaitUntil(() -> helper.assertTrue(pet.getMind().errands().isOn(CharacterPastimeData.GREET_OWNER),
                "the pet never went to greet its owner: it is " + pet.getMind().errands().errand()))
            .thenWaitUntil(() -> assertSaid(helper, pet, VoiceMoment.GREET))
            .thenSucceed();
    }

    private static ServerPlayer ownerAt(GameTestHelper helper, BlockPos rel) {
        ServerPlayer owner = player(helper);
        BlockPos at = helper.absolutePos(rel);
        owner.teleportTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        return owner;
    }

    private static AbstractPet ownedBy(GameTestHelper helper, EntityType<? extends AbstractPet> type, ServerPlayer owner, BlockPos at) {
        AbstractPet pet = GameTestKit.wild(helper, type, at);
        pet.tame(owner);
        pet.setPetDirective(PetDirective.FREE);
        return pet;
    }

    /** Another pet told to stay where it is, which is not free for a scene and so not about to play one with the pet beside it. */
    private static AbstractPet sittingBeside(GameTestHelper helper, ServerPlayer owner) {
        AbstractPet neighbour = ownedBy(helper, InitEntity.CHIIKAWA_PET.get(), owner, new BlockPos(6, STAND, 6));
        neighbour.setPetDirective(PetDirective.STAY);
        return neighbour;
    }
}
