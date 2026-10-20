package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.assertSaid;
import static com.dwinovo.chiikawa.gametest.GameTestKit.count;
import static com.dwinovo.chiikawa.gametest.GameTestKit.isLine;
import static com.dwinovo.chiikawa.gametest.GameTestKit.lastSaid;
import static com.dwinovo.chiikawa.gametest.GameTestKit.owned;
import static com.dwinovo.chiikawa.gametest.GameTestKit.player;
import static com.dwinovo.chiikawa.gametest.GameTestKit.quietYard;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;
import static com.dwinovo.chiikawa.gametest.GameTestKit.wild;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.data.PetInteractionData;
import com.dwinovo.chiikawa.data.PetTaskTypeData;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.entity.brain.PetMind;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.Outcome;
import com.dwinovo.chiikawa.entity.brain.errand.decide.DecisionLog;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.Message;
import com.dwinovo.chiikawa.init.InitDataComponents;
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.music.MusicBoxSelection;
import com.dwinovo.chiikawa.music.MusicTrackStatus;
import com.dwinovo.chiikawa.music.MusicTrackView;
import com.dwinovo.chiikawa.music.ServerMusicLibrary;
import com.dwinovo.chiikawa.music.ServerMusicSystem;
import com.dwinovo.chiikawa.social.InteractionPlan;
import com.dwinovo.chiikawa.social.PetInteraction;
import com.dwinovo.chiikawa.social.PetInteractions;
import com.dwinovo.chiikawa.social.SocialCooldowns;
import com.dwinovo.chiikawa.social.SocialRules;
import com.dwinovo.chiikawa.task.FinishedSlip;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Pets meeting pets: one walks over to another and the two play a little scene, then go
 * their own ways.
 *
 * <p>A pet only thinks of a scene now and then — each has a small chance every time it
 * looks around — so the cases hand the pet the idea themselves, exactly as its sensor
 * would, and watch what it does with it. Which scenes there are and who plays which part
 * comes from the generated data pack, the same files a player gets.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SocialGameTests {
    private static final String BATCH = "chiikawa_social";
    /** Long enough for every pet to have settled into pottering about. */
    private static final int SETTLE_TICKS = 20;
    /** Walking a few blocks, and a scene of a few seconds. */
    private static final int SCENE_TICKS = 400;
    /** How long a pet that should keep out of it is watched keeping out of it. */
    private static final int LEAVE_IT_TICKS = 100;
    /** A reservation made by hand, for a visitor that is never coming. */
    private static final int SHORT_WAIT = 60;
    /** A partner holding still shifts a little as it turns; this is more than that. */
    private static final double HELD_STILL = 1.5;
    /**
     * Importing a song, walking over, and the whole of the listening. Generous, because the
     * import runs off the server thread in its own time, and a test server does not wait
     * for it: it ticks as fast as it can in the meantime.
     */
    private static final int LISTEN_TICKS = 12000;
    /** The listening case's song: its file's name, which is also its title. */
    private static final String SONG = "chiikawa_gametest_busking";
    /** Longer than walking over and listening all the way through, so Hachiware is still playing after. */
    private static final int SONG_SECONDS = 50;

    /** The few seconds a pet that was turned down is held back from asking again. */
    private static final int TURNED_DOWN_REST_TICKS = 100;

    /** How far a rest may have run down between being made and being looked at. */
    private static final int RUN_DOWN_TICKS = 20;

    private static final int STAND = 2;

    @BeforeBatch(batch = BATCH)
    public static void settle(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    /**
     * Momonga goes over to Chiikawa, who stops, turns to it and waits — and while it
     * waits, nobody else can have it. Then both play their parts, and once the scene is
     * over Chiikawa is free again.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = SCENE_TICKS)
    public static void a_pet_goes_over_and_the_one_it_visits_plays_along(GameTestHelper helper) {
        AbstractPet momonga = wild(helper, InitEntity.MOMONGA_PET.get(), new BlockPos(4, STAND, 8));
        AbstractPet chiikawa = wild(helper, InitEntity.CHIIKAWA_PET.get(), new BlockPos(8, STAND, 8));
        // Another Momonga, told to sit so it never sets off itself, to ask whether it could.
        AbstractPet another = owned(helper, InitEntity.MOMONGA_PET.get(), new BlockPos(8, STAND, 11));
        another.setPetDirective(PetDirective.STAY);
        AtomicReference<Vec3> waitingAt = new AtomicReference<>();

        helper.startSequence()
            .thenIdle(SETTLE_TICKS)
            .thenExecute(() -> thinkOf(momonga, PetInteractionData.CLING, chiikawa))
            .thenWaitUntil(() -> {
                helper.assertTrue(reservedBy(chiikawa, momonga), "Chiikawa was never asked");
                helper.assertTrue(runs(chiikawa, ErrandIds.COOPERATE), "Chiikawa did not stop to play along");
                waitingAt.set(chiikawa.position());
            })
            .thenExecute(() -> helper.assertTrue(SocialRules.accepts(PetInteractionData.CLING, scene(PetInteractionData.CLING),
                    another, chiikawa, helper.getLevel().getGameTime()).isEmpty(),
                "another pet could have Chiikawa while it waited for Momonga"))
            .thenWaitUntil(() -> {
                helper.assertTrue("cling".equals(momonga.getPerformance()), "Momonga never clung on");
                helper.assertTrue("clung_to".equals(chiikawa.getPerformance()), "Chiikawa never played its part");
            })
            .thenExecute(() -> helper.assertTrue(chiikawa.position().distanceTo(waitingAt.get()) < HELD_STILL,
                "Chiikawa wandered off instead of waiting"))
            .thenWaitUntil(() -> helper.assertTrue(sceneOver(momonga, chiikawa), "the scene never ended"))
            .thenExecute(() -> helper.assertFalse(
                engaged(chiikawa),
                "Chiikawa was kept waiting after the scene"))
            .thenSucceed();
    }

    /**
     * A pet waits for a visitor only so long: one that never comes, and the partner goes
     * back to what it was doing.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = SETTLE_TICKS + SHORT_WAIT + 100)
    public static void a_partner_nobody_comes_for_goes_back_to_what_it_was_doing(GameTestHelper helper) {
        AbstractPet momonga = owned(helper, InitEntity.MOMONGA_PET.get(), new BlockPos(4, STAND, 8));
        momonga.setPetDirective(PetDirective.STAY);
        AbstractPet chiikawa = wild(helper, InitEntity.CHIIKAWA_PET.get(), new BlockPos(8, STAND, 8));

        helper.startSequence()
            .thenIdle(SETTLE_TICKS)
            .thenExecute(() -> {
                PetInteraction cling = scene(PetInteractionData.CLING);
                PetMind.post(chiikawa, new Message.Invitation(momonga.getUUID(), PetInteractionData.CLING,
                    cling.partnerRank(chiikawa.getType().builtInRegistryHolder()).getAsInt(),
                    ErrandSettings.of(ErrandIds.SOCIALIZE).orElseThrow().priority(),
                    helper.getLevel().getGameTime() + SHORT_WAIT));
            })
            .thenWaitUntil(() -> helper.assertTrue(runs(chiikawa, ErrandIds.COOPERATE),
                "Chiikawa did not wait for the pet it was told was coming"))
            .thenWaitUntil(() -> {
                helper.assertFalse(engaged(chiikawa),
                    "the reservation never ran out");
                helper.assertFalse(runs(chiikawa, ErrandIds.COOPERATE), "Chiikawa is still waiting for nobody");
            })
            .thenSucceed();
    }

    /**
     * A pet that cannot get to its partner — here Chiikawa up on a walled-in pillar — gives
     * up when its time runs out, lets go of the partner, and leaves that scene with that
     * pet alone for a while.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = SCENE_TICKS + 100)
    public static void a_pet_that_cannot_reach_its_partner_gives_up(GameTestHelper helper) {
        BlockPos perch = new BlockPos(10, STAND + 3, 8);
        for (int y = STAND; y < perch.getY(); y++) {
            helper.setBlock(new BlockPos(perch.getX(), y, perch.getZ()), Blocks.STONE);
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 || dz != 0) {
                    helper.setBlock(perch.offset(dx, 0, dz), Blocks.GLASS);
                    helper.setBlock(perch.offset(dx, 1, dz), Blocks.GLASS);
                }
            }
        }
        AbstractPet momonga = wild(helper, InitEntity.MOMONGA_PET.get(), new BlockPos(7, STAND, 8));
        AbstractPet chiikawa = wild(helper, InitEntity.CHIIKAWA_PET.get(), perch);

        helper.startSequence()
            .thenIdle(SETTLE_TICKS)
            .thenExecute(() -> thinkOf(momonga, PetInteractionData.CLING, chiikawa))
            .thenWaitUntil(() -> helper.assertTrue(reservedBy(chiikawa, momonga), "Chiikawa was never asked"))
            .thenWaitUntil(() -> {
                helper.assertFalse(engaged(chiikawa),
                    "Momonga never let go of Chiikawa");
                helper.assertFalse(momonga.getBrain().hasMemoryValue(InitMemory.INTERACTION_PLAN.get()),
                    "Momonga is still set on a pet it cannot reach");
            })
            .thenExecute(() -> {
                helper.assertTrue(momonga.getPerformance().isEmpty(), "Momonga played the scene through the glass");
                helper.assertTrue(coolingDown(momonga, PetInteractionData.CLING, chiikawa, helper),
                    "Momonga would go straight back to try again");
            })
            .thenSucceed();
    }

    /** Told to sit, a pet stays sitting, however much it would like to go and cling on. */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = SETTLE_TICKS + LEAVE_IT_TICKS + 20)
    public static void a_sitting_pet_does_not_go_visiting(GameTestHelper helper) {
        AbstractPet momonga = owned(helper, InitEntity.MOMONGA_PET.get(), new BlockPos(4, STAND, 8));
        momonga.setPetDirective(PetDirective.STAY);
        AbstractPet chiikawa = wild(helper, InitEntity.CHIIKAWA_PET.get(), new BlockPos(8, STAND, 8));

        helper.startSequence()
            .thenIdle(SETTLE_TICKS)
            .thenExecute(() -> thinkOf(momonga, PetInteractionData.CLING, chiikawa))
            .thenExecuteFor(LEAVE_IT_TICKS, () -> {
                helper.assertTrue(runs(momonga, ErrandIds.STAY), "a sitting pet got up");
                helper.assertFalse(engaged(chiikawa),
                    "a sitting pet asked another over");
            })
            .thenSucceed();
    }

    /** Nor is a sitting pet somebody else's to take: it is left sitting. */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = SETTLE_TICKS + LEAVE_IT_TICKS + 20)
    public static void nobody_takes_a_sitting_pet_away(GameTestHelper helper) {
        AbstractPet momonga = wild(helper, InitEntity.MOMONGA_PET.get(), new BlockPos(4, STAND, 8));
        AbstractPet chiikawa = owned(helper, InitEntity.CHIIKAWA_PET.get(), new BlockPos(8, STAND, 8));
        chiikawa.setPetDirective(PetDirective.STAY);

        helper.startSequence()
            .thenIdle(SETTLE_TICKS)
            .thenExecute(() -> helper.assertTrue(SocialRules.accepts(PetInteractionData.CLING,
                    scene(PetInteractionData.CLING), momonga, chiikawa, helper.getLevel().getGameTime()).isEmpty(),
                "a sitting pet was up for a scene"))
            .thenExecute(() -> thinkOf(momonga, PetInteractionData.CLING, chiikawa))
            .thenExecuteFor(LEAVE_IT_TICKS, () -> {
                helper.assertTrue(runs(chiikawa, ErrandIds.STAY), "a sitting pet was made to get up");
                helper.assertFalse(engaged(chiikawa),
                    "a sitting pet was asked over");
                helper.assertTrue(momonga.getPerformance().isEmpty(), "Momonga clung onto a sitting pet");
            })
            .thenSucceed();
    }

    /**
     * Once two pets have played a scene, they leave that one alone for a while — though
     * the same pet may still play it with somebody else.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = SCENE_TICKS + 200)
    public static void the_same_two_do_not_play_the_same_scene_again_straight_away(GameTestHelper helper) {
        AbstractPet momonga = wild(helper, InitEntity.MOMONGA_PET.get(), new BlockPos(4, STAND, 8));
        AbstractPet chiikawa = wild(helper, InitEntity.CHIIKAWA_PET.get(), new BlockPos(8, STAND, 8));
        AtomicReference<AbstractPet> newcomer = new AtomicReference<>();

        helper.startSequence()
            .thenIdle(SETTLE_TICKS)
            .thenExecute(() -> thinkOf(momonga, PetInteractionData.CLING, chiikawa))
            .thenWaitUntil(() -> helper.assertTrue("clung_to".equals(chiikawa.getPerformance()), "the scene never began"))
            .thenWaitUntil(() -> helper.assertTrue(sceneOver(momonga, chiikawa), "the scene never ended"))
            .thenExecute(() -> newcomer.set(wild(helper, InitEntity.CHIIKAWA_PET.get(), new BlockPos(4, STAND, 11))))
            .thenExecute(() -> {
                helper.assertTrue(coolingDown(momonga, PetInteractionData.CLING, chiikawa, helper),
                    "Momonga does not remember having clung to Chiikawa");
                helper.assertTrue(coolingDown(chiikawa, PetInteractionData.CLING, momonga, helper),
                    "Chiikawa does not remember having been clung to by Momonga");
            })
            .thenWaitUntil(() -> {
                helper.assertTrue(runs(newcomer.get(), ErrandIds.WANDER), "the newcomer never settled");
                long now = helper.getLevel().getGameTime();
                PetInteraction cling = scene(PetInteractionData.CLING);
                helper.assertTrue(SocialRules.accepts(PetInteractionData.CLING, cling, momonga, chiikawa, now).isEmpty(),
                    "Momonga would cling to Chiikawa again at once");
                helper.assertTrue(SocialRules.accepts(PetInteractionData.CLING, cling, momonga, newcomer.get(), now)
                    .isPresent(), "Momonga would not cling to somebody new either");
            })
            .thenExecute(() -> thinkOf(momonga, PetInteractionData.CLING, chiikawa))
            .thenExecuteFor(LEAVE_IT_TICKS, () -> helper.assertFalse(reservedBy(chiikawa, momonga),
                "Momonga went back to Chiikawa for the same scene"))
            .thenSucceed();
    }

    /**
     * A pet that is turned down, or is never answered, has not played anything: it is held back from asking the same
     * pet only for a few seconds, so that it is not asking again in the very next breath, and not for the day that a
     * scene played rests the pair.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = SETTLE_TICKS + SCENE_TICKS + TURNED_DOWN_REST_TICKS)
    public static void a_pet_turned_down_may_ask_again_in_a_few_seconds_and_not_a_day(GameTestHelper helper) {
        AbstractPet momonga = wild(helper, InitEntity.MOMONGA_PET.get(), new BlockPos(4, STAND, 8));
        AbstractPet chiikawa = wild(helper, InitEntity.CHIIKAWA_PET.get(), new BlockPos(8, STAND, 8));
        PetInteraction cling = scene(PetInteractionData.CLING);

        helper.startSequence()
            .thenIdle(SETTLE_TICKS)
            .thenExecute(() -> {
                // Chiikawa is resting from Momonga's scene, so it says no to being asked, as a pet that has got busy since
                // Momonga thought of it would; Momonga, who knows nothing of that, sets off to ask.
                chiikawa.getBrain().setMemory(InitMemory.SOCIAL_COOLDOWNS.get(), SocialCooldowns.NONE.with(
                    SocialCooldowns.subject(PetInteractionData.CLING, momonga.getUUID()), cling.cooldownTicks(),
                    helper.getLevel().getGameTime()));
                thinkOf(momonga, PetInteractionData.CLING, chiikawa);
            })
            .thenWaitUntil(() -> helper.assertTrue(endedWith(momonga, ErrandIds.SOCIALIZE, "declined"),
                "Momonga was never turned down"))
            .thenExecute(() -> {
                long left = restLeft(momonga, PetInteractionData.CLING, chiikawa, helper);
                helper.assertTrue(left > 0 && left <= TURNED_DOWN_REST_TICKS,
                    "Momonga is held back from Chiikawa for " + left + " ticks, not the few seconds of a refusal");
                helper.assertTrue(cling.cooldownTicks() > TURNED_DOWN_REST_TICKS * 10,
                    "a day is not very different from a few seconds in this data pack");
            })
            .thenIdle(TURNED_DOWN_REST_TICKS)
            .thenExecute(() -> helper.assertTrue(SocialRules.accepts(PetInteractionData.CLING, cling, momonga, chiikawa,
                    helper.getLevel().getGameTime()).isPresent(),
                "Momonga is still held back from asking Chiikawa, " + restLeft(momonga, PetInteractionData.CLING, chiikawa, helper)
                    + " ticks later"))
            .thenSucceed();
    }

    /**
     * A scene that has begun rests the pair for its whole cooldown, though one of them is taken away a moment later:
     * the rest is made as the scene begins, and not left to however it is ended.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = SCENE_TICKS)
    public static void a_scene_cut_short_after_it_began_rests_the_pair_for_its_whole_cooldown(GameTestHelper helper) {
        AbstractPet momonga = wild(helper, InitEntity.MOMONGA_PET.get(), new BlockPos(4, STAND, 8));
        AbstractPet chiikawa = wild(helper, InitEntity.CHIIKAWA_PET.get(), new BlockPos(8, STAND, 8));
        long cooldown = scene(PetInteractionData.CLING).cooldownTicks();

        helper.startSequence()
            .thenIdle(SETTLE_TICKS)
            .thenExecute(() -> thinkOf(momonga, PetInteractionData.CLING, chiikawa))
            .thenWaitUntil(() -> helper.assertTrue("cling".equals(momonga.getPerformance())
                    && "clung_to".equals(chiikawa.getPerformance()), "the scene never began"))
            .thenExecute(() -> PetMind.post(momonga, new Message.Moved("test")))
            .thenWaitUntil(() -> helper.assertTrue(sceneOver(momonga, chiikawa), "the scene was never ended"))
            .thenExecute(() -> {
                helper.assertTrue(restLeft(momonga, PetInteractionData.CLING, chiikawa, helper) > cooldown - RUN_DOWN_TICKS,
                    "Momonga, who was taken away, rests from Chiikawa for only "
                        + restLeft(momonga, PetInteractionData.CLING, chiikawa, helper) + " of the " + cooldown + " ticks");
                helper.assertTrue(restLeft(chiikawa, PetInteractionData.CLING, momonga, helper) > cooldown - RUN_DOWN_TICKS,
                    "Chiikawa, who was left, rests from Momonga for only "
                        + restLeft(chiikawa, PetInteractionData.CLING, momonga, helper) + " of the " + cooldown + " ticks");
            })
            .thenSucceed();
    }

    /**
     * Rakko hands over something to eat out of its own backpack, and Chiikawa eats it there
     * and then: one fewer in Rakko's things, a little better for Chiikawa.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = SCENE_TICKS)
    public static void a_treat_changes_hands_and_is_eaten(GameTestHelper helper) {
        AbstractPet rakko = wild(helper, InitEntity.RAKKO_PET.get(), new BlockPos(4, STAND, 8));
        rakko.getBackpack().setItem(AbstractPet.BAG_SLOT + 1, new ItemStack(Items.COOKIE, 2));
        AbstractPet chiikawa = wild(helper, InitEntity.CHIIKAWA_PET.get(), new BlockPos(8, STAND, 8));
        chiikawa.setHealth(chiikawa.getMaxHealth() / 2);
        float hungry = chiikawa.getHealth();

        helper.startSequence()
            .thenIdle(SETTLE_TICKS)
            .thenExecute(() -> thinkOf(rakko, PetInteractionData.TREAT, chiikawa))
            .thenWaitUntil(() -> helper.assertTrue("eat".equals(chiikawa.getPerformance()), "Chiikawa was never treated"))
            .thenExecute(() -> {
                helper.assertTrue(count(rakko, Items.COOKIE) == 1, "Rakko did not hand exactly one over");
                helper.assertTrue(chiikawa.getHealth() > hungry, "Chiikawa did not eat what it was given");
            })
            .thenSucceed();
    }

    /**
     * Kurimanju only brings coffee to somebody fresh off a weeding slip, and whoever gets it
     * is keener on work for a while.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = SCENE_TICKS)
    public static void coffee_goes_to_whoever_has_just_weeded(GameTestHelper helper) {
        AbstractPet kurimanju = wild(helper, InitEntity.KURIMANJU_PET.get(), new BlockPos(4, STAND, 8));
        AbstractPet chiikawa = wild(helper, InitEntity.CHIIKAWA_PET.get(), new BlockPos(8, STAND, 8));

        helper.startSequence()
            .thenIdle(SETTLE_TICKS)
            .thenExecute(() -> {
                long now = helper.getLevel().getGameTime();
                helper.assertTrue(SocialRules.accepts(PetInteractionData.COFFEE, scene(PetInteractionData.COFFEE),
                    kurimanju, chiikawa, now).isEmpty(), "coffee for somebody who has not been working");
                chiikawa.getBrain().setMemory(InitMemory.LAST_FINISHED_SLIP.get(),
                    new FinishedSlip(PetTaskTypeData.WEEDING, now));
                thinkOf(kurimanju, PetInteractionData.COFFEE, chiikawa);
            })
            .thenWaitUntil(() -> helper.assertTrue("drink".equals(chiikawa.getPerformance()), "the coffee never came"))
            .thenExecute(() -> helper.assertTrue(chiikawa.isEager(), "the coffee did not perk Chiikawa up"))
            .thenSucceed();
    }

    /**
     * Nobody praises Momonga, so as it lets go it bursts into its fake tears and wants
     * comforting instead: the scene's closing beat. A batch of its own, in a hushed yard,
     * since what is being watched for is a line.
     */
    @GameTest(template = "floor16", batch = "chiikawa_social_turned_down", timeoutTicks = SCENE_TICKS)
    public static void momonga_let_go_unpraised_wants_comforting(GameTestHelper helper) {
        quietYard(helper, NOON);
        AbstractPet momonga = wild(helper, InitEntity.MOMONGA_PET.get(), new BlockPos(4, STAND, 8));
        AbstractPet chiikawa = wild(helper, InitEntity.CHIIKAWA_PET.get(), new BlockPos(8, STAND, 8));

        helper.startSequence()
            .thenIdle(SETTLE_TICKS)
            .thenExecute(() -> thinkOf(momonga, PetInteractionData.CLING, chiikawa))
            .thenWaitUntil(() -> helper.assertTrue("clung_to".equals(chiikawa.getPerformance()), "the scene never began"))
            .thenWaitUntil(() -> helper.assertTrue(sceneOver(momonga, chiikawa), "the scene never ended"))
            .thenExecute(() -> assertSaid(helper, momonga, VoiceMoment.TURNED_DOWN))
            .thenSucceed();
    }

    /**
     * Hachiware busks a real song off its music box, and Chiikawa sits down by it to listen
     * and now and then claps along — with a word, which is what the case can hear: its
     * listening line said again after the one it sat down with. Once the scene is over it
     * gets up and Hachiware plays on.
     *
     * <p>The song is written into the test server's music folder the way a player adds one,
     * and imported like any other; it is silence, which is all the server ever looks at.
     */
    @GameTest(template = "floor16", batch = "chiikawa_social_listen", timeoutTicks = LISTEN_TICKS)
    public static void a_listener_sits_by_the_busker_and_claps_now_and_then(GameTestHelper helper) {
        quietYard(helper, NOON);
        ServerMusicLibrary library = ServerMusicSystem.library(helper.getLevel().getServer());
        GameTestKit.addSong(library, SONG, SONG_SECONDS);
        AtomicReference<AbstractPet> hachiware = new AtomicReference<>();
        AtomicReference<AbstractPet> chiikawa = new AtomicReference<>();
        AtomicLong satDown = new AtomicLong();

        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(GameTestKit.readySong(library, SONG).isPresent(), "the song was never imported"))
            .thenExecute(() -> {
                ItemStack box = new ItemStack(InitItems.MUSIC_BOX.get());
                box.set(InitDataComponents.MUSIC_BOX_SELECTION.get(),
                    new MusicBoxSelection(GameTestKit.readySong(library, SONG).orElseThrow().trackId(), SONG, 0));
                // Somebody's, and let loose: playing is work, which a wild pet does not do.
                AbstractPet busker = owned(helper, InitEntity.HACHIWARE_PET.get(), new BlockPos(8, STAND, 8));
                busker.setItemSlot(EquipmentSlot.MAINHAND, box);
                hachiware.set(busker);
                chiikawa.set(wild(helper, InitEntity.CHIIKAWA_PET.get(), new BlockPos(3, STAND, 8)));
            })
            .thenWaitUntil(() -> helper.assertTrue(hachiware.get().getActivity() == PetActivity.PLAY_GUITAR,
                "Hachiware never started playing"))
            .thenExecute(() -> thinkOf(chiikawa.get(), PetInteractionData.LISTEN_TO_MUSIC, hachiware.get()))
            .thenWaitUntil(() -> {
                helper.assertTrue("sit".equals(chiikawa.get().getPerformance()), "Chiikawa never sat down to listen");
                satDown.set(helper.getLevel().getGameTime());
            })
            .thenWaitUntil(() -> helper.assertTrue(lastSaid(chiikawa.get())
                    .filter(said -> said.gameTime() > satDown.get())
                    .filter(said -> isLine(chiikawa.get(), VoiceMoment.LISTEN, said.line()))
                    .isPresent(),
                "Chiikawa never clapped along"))
            .thenWaitUntil(() -> helper.assertTrue(chiikawa.get().getPerformance().isEmpty()
                    && !chiikawa.get().getBrain().hasMemoryValue(InitMemory.INTERACTION_PLAN.get()),
                "Chiikawa never got up again"))
            .thenExecute(() -> helper.assertTrue(hachiware.get().getActivity() == PetActivity.PLAY_GUITAR,
                "the audience leaving stopped the music"))
            .thenSucceed();
    }

    /**
     * Someone whose game cannot take the mod's packets stands right by the busker: the song
     * goes on, and nothing is sent to them — the stand-in player a case makes is just such
     * a game.
     */
    @GameTest(template = "floor16", batch = "chiikawa_social_listen", timeoutTicks = LISTEN_TICKS)
    public static void the_busker_plays_on_beside_a_game_that_cannot_hear_it(GameTestHelper helper) {
        quietYard(helper, NOON);
        ServerMusicLibrary library = ServerMusicSystem.library(helper.getLevel().getServer());
        GameTestKit.addSong(library, SONG, SONG_SECONDS);
        AtomicReference<AbstractPet> hachiware = new AtomicReference<>();
        AtomicLong started = new AtomicLong();

        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(GameTestKit.readySong(library, SONG).isPresent(), "the song was never imported"))
            .thenExecute(() -> {
                ServerPlayer bystander = player(helper);
                bystander.moveTo(helper.absoluteVec(new Vec3(9.5, STAND, 8.5)));
                ItemStack box = new ItemStack(InitItems.MUSIC_BOX.get());
                box.set(InitDataComponents.MUSIC_BOX_SELECTION.get(),
                    new MusicBoxSelection(GameTestKit.readySong(library, SONG).orElseThrow().trackId(), SONG, 0));
                AbstractPet busker = owned(helper, InitEntity.HACHIWARE_PET.get(), new BlockPos(8, STAND, 8));
                busker.setItemSlot(EquipmentSlot.MAINHAND, box);
                hachiware.set(busker);
            })
            .thenWaitUntil(() -> {
                helper.assertTrue(hachiware.get().getActivity() == PetActivity.PLAY_GUITAR, "Hachiware never started playing");
                started.set(helper.getLevel().getGameTime());
            })
            .thenWaitUntil(() -> helper.assertTrue(helper.getLevel().getGameTime() - started.get() >= 40,
                "the song has not been playing a while yet"))
            .thenExecute(() -> helper.assertTrue(hachiware.get().getActivity() == PetActivity.PLAY_GUITAR,
                "the music stopped with somebody standing by"))
            .thenSucceed();
    }

    /** Rakko and Kurimanju listen without a fuss: their part has no clapping in it. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 20)
    public static void rakko_and_kurimanju_listen_without_clapping(GameTestHelper helper) {
        PetInteraction listen = scene(PetInteractionData.LISTEN_TO_MUSIC);
        for (EntityType<?> composed : List.of(InitEntity.RAKKO_PET.get(), InitEntity.KURIMANJU_PET.get())) {
            PetInteraction.Side part = listen.initiatorSide(composed.builtInRegistryHolder()).orElseThrow();
            helper.assertTrue(part.nowAndThen().isEmpty(), composed.toShortString() + " claps along");
        }
        helper.assertTrue(listen.initiatorSide(InitEntity.CHIIKAWA_PET.get().builtInRegistryHolder()).orElseThrow()
            .nowAndThen().isPresent(), "nobody claps at all");
        helper.succeed();
    }

    /**
     * Gives a pet the idea its sensor would have had: this scene, with this partner. A pet
     * that has already set off on it by itself is left to get on with it.
     */
    private static void thinkOf(AbstractPet pet, ResourceLocation id, AbstractPet partner) {
        if (pet.getMind().errands().isOn(ErrandIds.SOCIALIZE)) {
            return;
        }
        PetInteraction interaction = scene(id);
        pet.getBrain().setMemory(InitMemory.INTERACTION_PLAN.get(), new InteractionPlan(id, interaction,
            interaction.initiatorSide(pet.getType().builtInRegistryHolder()).orElseThrow(), partner,
            interaction.partners().get(interaction.partnerRank(partner.getType().builtInRegistryHolder()).getAsInt())));
    }

    private static PetInteraction scene(ResourceLocation id) {
        PetInteraction interaction = PetInteractions.all().get(id);
        if (interaction == null) {
            throw new AssertionError("the data pack has no " + id);
        }
        return interaction;
    }

    /** Both have stopped playing, and the one that came over has let the idea go. */
    private static boolean sceneOver(AbstractPet pet, AbstractPet partner) {
        return pet.getPerformance().isEmpty() && partner.getPerformance().isEmpty()
            && !pet.getBrain().hasMemoryValue(InitMemory.INTERACTION_PLAN.get());
    }

    private static boolean reservedBy(AbstractPet partner, AbstractPet pet) {
        return partner.getMind().errands().engagedWith(partner.level().getGameTime()).filter(pet.getUUID()::equals).isPresent();
    }

    /** Said yes to somebody and not yet let go of. */
    private static boolean engaged(AbstractPet pet) {
        return pet.getMind().errands().engagedWith(pet.level().getGameTime()).isPresent();
    }

    private static boolean runs(AbstractPet pet, ResourceLocation intent) {
        return pet.getIntent().filter(intent::equals).isPresent();
    }

    /** @return how long the pet has left of resting from this scene with that pet, 0 for none */
    private static long restLeft(AbstractPet pet, ResourceLocation id, AbstractPet other, GameTestHelper helper) {
        return pet.getBrain().getMemory(InitMemory.SOCIAL_COOLDOWNS.get())
            .orElse(SocialCooldowns.NONE)
            .remaining(SocialCooldowns.subject(id, other.getUUID()), helper.getLevel().getGameTime());
    }

    /** @return whether the errand has ended with the pet giving up for this reason */
    private static boolean endedWith(AbstractPet pet, ResourceLocation errand, String reason) {
        return pet.getMind().log().entries().stream().anyMatch(entry -> entry instanceof DecisionLog.Ended ended
            && ended.errand().equals(errand) && ended.outcome() instanceof Outcome.GaveUp gaveUp && gaveUp.reason().equals(reason));
    }

    private static boolean coolingDown(AbstractPet pet, ResourceLocation id, AbstractPet other, GameTestHelper helper) {
        return pet.getBrain().getMemory(InitMemory.SOCIAL_COOLDOWNS.get())
            .orElse(SocialCooldowns.NONE)
            .coolingDown(id, other.getUUID(), helper.getLevel().getGameTime());
    }
}
