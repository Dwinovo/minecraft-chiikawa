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
import com.dwinovo.chiikawa.entity.brain.intent.IntentSelector;
import com.dwinovo.chiikawa.entity.brain.intent.PetIntents;
import com.dwinovo.chiikawa.entity.brain.intent.RunningIntent;
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
import com.dwinovo.chiikawa.social.InteractionReservation;
import com.dwinovo.chiikawa.social.PetInteraction;
import com.dwinovo.chiikawa.social.PetInteractions;
import com.dwinovo.chiikawa.social.SocialCooldowns;
import com.dwinovo.chiikawa.social.SocialRules;
import com.dwinovo.chiikawa.task.FinishedSlip;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

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
    /** Low, to keep the file small; the library resamples it like any other. */
    private static final int SONG_RATE = 8000;

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
                helper.assertTrue(reservedBy(chiikawa, momonga), Component.literal("Chiikawa was never asked"));
                helper.assertTrue(runs(chiikawa, PetIntents.COOPERATE), Component.literal("Chiikawa did not stop to play along"));
                waitingAt.set(chiikawa.position());
            })
            .thenExecute(() -> helper.assertTrue(SocialRules.accepts(PetInteractionData.CLING, scene(PetInteractionData.CLING),
                    another, chiikawa, helper.getLevel().getGameTime()).isEmpty(),
                Component.literal("another pet could have Chiikawa while it waited for Momonga")))
            .thenWaitUntil(() -> {
                helper.assertTrue("cling".equals(momonga.getPerformance()), Component.literal("Momonga never clung on"));
                helper.assertTrue("clung_to".equals(chiikawa.getPerformance()), Component.literal("Chiikawa never played its part"));
            })
            .thenExecute(() -> helper.assertTrue(chiikawa.position().distanceTo(waitingAt.get()) < HELD_STILL,
                Component.literal("Chiikawa wandered off instead of waiting")))
            .thenWaitUntil(() -> helper.assertTrue(sceneOver(momonga, chiikawa), Component.literal("the scene never ended")))
            .thenExecute(() -> helper.assertFalse(
                chiikawa.getBrain().hasMemoryValue(InitMemory.INTERACTION_RESERVATION.get()),
                Component.literal("Chiikawa was kept waiting after the scene")))
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
                chiikawa.getBrain().setMemoryWithExpiry(InitMemory.INTERACTION_RESERVATION.get(),
                    new InteractionReservation(momonga, cling.partners().get(0), false), SHORT_WAIT);
                IntentSelector.requestReevaluate(chiikawa);
            })
            .thenWaitUntil(() -> helper.assertTrue(runs(chiikawa, PetIntents.COOPERATE),
                Component.literal("Chiikawa did not wait for the pet it was told was coming")))
            .thenWaitUntil(() -> {
                helper.assertFalse(chiikawa.getBrain().hasMemoryValue(InitMemory.INTERACTION_RESERVATION.get()),
                    Component.literal("the reservation never ran out"));
                helper.assertFalse(runs(chiikawa, PetIntents.COOPERATE), Component.literal("Chiikawa is still waiting for nobody"));
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
            .thenWaitUntil(() -> helper.assertTrue(reservedBy(chiikawa, momonga), Component.literal("Chiikawa was never asked")))
            .thenWaitUntil(() -> {
                helper.assertFalse(chiikawa.getBrain().hasMemoryValue(InitMemory.INTERACTION_RESERVATION.get()),
                    Component.literal("Momonga never let go of Chiikawa"));
                helper.assertFalse(momonga.getBrain().hasMemoryValue(InitMemory.INTERACTION_PLAN.get()),
                    Component.literal("Momonga is still set on a pet it cannot reach"));
            })
            .thenExecute(() -> {
                helper.assertTrue(momonga.getPerformance().isEmpty(), Component.literal("Momonga played the scene through the glass"));
                helper.assertTrue(coolingDown(momonga, PetInteractionData.CLING, chiikawa, helper),
                    Component.literal("Momonga would go straight back to try again"));
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
                helper.assertTrue(runs(momonga, PetIntents.STAY), Component.literal("a sitting pet got up"));
                helper.assertFalse(chiikawa.getBrain().hasMemoryValue(InitMemory.INTERACTION_RESERVATION.get()),
                    Component.literal("a sitting pet asked another over"));
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
                Component.literal("a sitting pet was up for a scene")))
            .thenExecute(() -> thinkOf(momonga, PetInteractionData.CLING, chiikawa))
            .thenExecuteFor(LEAVE_IT_TICKS, () -> {
                helper.assertTrue(runs(chiikawa, PetIntents.STAY), Component.literal("a sitting pet was made to get up"));
                helper.assertFalse(chiikawa.getBrain().hasMemoryValue(InitMemory.INTERACTION_RESERVATION.get()),
                    Component.literal("a sitting pet was asked over"));
                helper.assertTrue(momonga.getPerformance().isEmpty(), Component.literal("Momonga clung onto a sitting pet"));
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
            .thenWaitUntil(() -> helper.assertTrue("clung_to".equals(chiikawa.getPerformance()), Component.literal("the scene never began")))
            .thenWaitUntil(() -> helper.assertTrue(sceneOver(momonga, chiikawa), Component.literal("the scene never ended")))
            .thenExecute(() -> newcomer.set(wild(helper, InitEntity.CHIIKAWA_PET.get(), new BlockPos(4, STAND, 11))))
            .thenExecute(() -> {
                helper.assertTrue(coolingDown(momonga, PetInteractionData.CLING, chiikawa, helper),
                    Component.literal("Momonga does not remember having clung to Chiikawa"));
                helper.assertTrue(coolingDown(chiikawa, PetInteractionData.CLING, momonga, helper),
                    Component.literal("Chiikawa does not remember having been clung to by Momonga"));
            })
            .thenWaitUntil(() -> {
                helper.assertTrue(runs(newcomer.get(), PetIntents.WANDER), Component.literal("the newcomer never settled"));
                long now = helper.getLevel().getGameTime();
                PetInteraction cling = scene(PetInteractionData.CLING);
                helper.assertTrue(SocialRules.accepts(PetInteractionData.CLING, cling, momonga, chiikawa, now).isEmpty(),
                    Component.literal("Momonga would cling to Chiikawa again at once"));
                helper.assertTrue(SocialRules.accepts(PetInteractionData.CLING, cling, momonga, newcomer.get(), now)
                    .isPresent(), Component.literal("Momonga would not cling to somebody new either"));
            })
            .thenExecute(() -> thinkOf(momonga, PetInteractionData.CLING, chiikawa))
            .thenExecuteFor(LEAVE_IT_TICKS, () -> helper.assertFalse(reservedBy(chiikawa, momonga),
                Component.literal("Momonga went back to Chiikawa for the same scene")))
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
            .thenWaitUntil(() -> helper.assertTrue("eat".equals(chiikawa.getPerformance()), Component.literal("Chiikawa was never treated")))
            .thenExecute(() -> {
                helper.assertTrue(count(rakko, Items.COOKIE) == 1, Component.literal("Rakko did not hand exactly one over"));
                helper.assertTrue(chiikawa.getHealth() > hungry, Component.literal("Chiikawa did not eat what it was given"));
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
                    kurimanju, chiikawa, now).isEmpty(), Component.literal("coffee for somebody who has not been working"));
                chiikawa.getBrain().setMemory(InitMemory.LAST_FINISHED_SLIP.get(),
                    new FinishedSlip(PetTaskTypeData.WEEDING, now));
                thinkOf(kurimanju, PetInteractionData.COFFEE, chiikawa);
            })
            .thenWaitUntil(() -> helper.assertTrue("drink".equals(chiikawa.getPerformance()), Component.literal("the coffee never came")))
            .thenExecute(() -> helper.assertTrue(chiikawa.isEager(), Component.literal("the coffee did not perk Chiikawa up")))
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
            .thenWaitUntil(() -> helper.assertTrue("clung_to".equals(chiikawa.getPerformance()), Component.literal("the scene never began")))
            .thenWaitUntil(() -> helper.assertTrue(sceneOver(momonga, chiikawa), Component.literal("the scene never ended")))
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
        addSong(library);
        AtomicReference<AbstractPet> hachiware = new AtomicReference<>();
        AtomicReference<AbstractPet> chiikawa = new AtomicReference<>();
        AtomicLong satDown = new AtomicLong();

        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(song(library).isPresent(), Component.literal("the song was never imported")))
            .thenExecute(() -> {
                ItemStack box = new ItemStack(InitItems.MUSIC_BOX.get());
                box.set(InitDataComponents.MUSIC_BOX_SELECTION.get(),
                    new MusicBoxSelection(song(library).orElseThrow().trackId(), SONG, 0));
                // Somebody's, and let loose: playing is work, which a wild pet does not do.
                AbstractPet busker = owned(helper, InitEntity.HACHIWARE_PET.get(), new BlockPos(8, STAND, 8));
                busker.setItemSlot(EquipmentSlot.MAINHAND, box);
                hachiware.set(busker);
                chiikawa.set(wild(helper, InitEntity.CHIIKAWA_PET.get(), new BlockPos(3, STAND, 8)));
            })
            .thenWaitUntil(() -> helper.assertTrue(hachiware.get().getActivity() == PetActivity.PLAY_GUITAR,
                Component.literal("Hachiware never started playing")))
            .thenExecute(() -> thinkOf(chiikawa.get(), PetInteractionData.LISTEN_TO_MUSIC, hachiware.get()))
            .thenWaitUntil(() -> {
                helper.assertTrue("sit".equals(chiikawa.get().getPerformance()), Component.literal("Chiikawa never sat down to listen"));
                satDown.set(helper.getLevel().getGameTime());
            })
            .thenWaitUntil(() -> helper.assertTrue(lastSaid(chiikawa.get())
                    .filter(said -> said.gameTime() > satDown.get())
                    .filter(said -> isLine(chiikawa.get(), VoiceMoment.LISTEN, said.line()))
                    .isPresent(),
                Component.literal("Chiikawa never clapped along")))
            .thenWaitUntil(() -> helper.assertTrue(chiikawa.get().getPerformance().isEmpty()
                    && !chiikawa.get().getBrain().hasMemoryValue(InitMemory.INTERACTION_PLAN.get()),
                Component.literal("Chiikawa never got up again")))
            .thenExecute(() -> helper.assertTrue(hachiware.get().getActivity() == PetActivity.PLAY_GUITAR,
                Component.literal("the audience leaving stopped the music")))
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
        addSong(library);
        AtomicReference<AbstractPet> hachiware = new AtomicReference<>();
        AtomicLong started = new AtomicLong();

        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(song(library).isPresent(), "the song was never imported"))
            .thenExecute(() -> {
                ServerPlayer bystander = player(helper);
                bystander.moveTo(helper.absoluteVec(new Vec3(9.5, STAND, 8.5)));
                ItemStack box = new ItemStack(InitItems.MUSIC_BOX.get());
                box.set(InitDataComponents.MUSIC_BOX_SELECTION.get(),
                    new MusicBoxSelection(song(library).orElseThrow().trackId(), SONG, 0));
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
            helper.assertTrue(part.nowAndThen().isEmpty(), Component.literal(composed.toShortString() + " claps along"));
        }
        helper.assertTrue(listen.initiatorSide(InitEntity.CHIIKAWA_PET.get().builtInRegistryHolder()).orElseThrow()
            .nowAndThen().isPresent(), Component.literal("nobody claps at all"));
        helper.succeed();
    }

    /**
     * Writes the song the listening case plays into the server's music folder, unless an
     * earlier run left it there, and has the library look again.
     */
    private static void addSong(ServerMusicLibrary library) {
        Path file = library.musicDir().resolve(SONG + ".wav");
        try {
            if (!Files.exists(file)) {
                AudioFormat format = new AudioFormat(SONG_RATE, 16, 1, true, false);
                byte[] silence = new byte[SONG_RATE * 2 * SONG_SECONDS];
                try (AudioInputStream song = new AudioInputStream(new ByteArrayInputStream(silence), format,
                        silence.length / 2)) {
                    AudioSystem.write(song, AudioFileFormat.Type.WAVE, file.toFile());
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        library.rescan();
    }

    /** The case's song, once it is ready to play. */
    private static Optional<MusicTrackView> song(ServerMusicLibrary library) {
        return library.catalog().stream()
            .filter(track -> track.title().equals(SONG) && track.status() == MusicTrackStatus.READY)
            .findFirst();
    }

    /**
     * Gives a pet the idea its sensor would have had: this scene, with this partner. A pet
     * that has already set off on it by itself is left to get on with it.
     */
    private static void thinkOf(AbstractPet pet, Identifier id, AbstractPet partner) {
        if (pet.getBrain().getMemory(InitMemory.INTERACTION_PLAN.get()).filter(InteractionPlan::engaged).isPresent()) {
            return;
        }
        PetInteraction interaction = scene(id);
        pet.getBrain().setMemory(InitMemory.INTERACTION_PLAN.get(), new InteractionPlan(id, interaction,
            interaction.initiatorSide(pet.getType().builtInRegistryHolder()).orElseThrow(), partner,
            interaction.partners().get(interaction.partnerRank(partner.getType().builtInRegistryHolder()).getAsInt()),
            false));
    }

    private static PetInteraction scene(Identifier id) {
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
        return partner.getBrain().getMemory(InitMemory.INTERACTION_RESERVATION.get())
            .filter(reservation -> reservation.heldBy(pet))
            .isPresent();
    }

    private static boolean runs(AbstractPet pet, Identifier intent) {
        return pet.getBrain().getMemory(InitMemory.CURRENT_INTENT.get())
            .map(RunningIntent::id)
            .filter(intent::equals)
            .isPresent();
    }

    private static boolean coolingDown(AbstractPet pet, Identifier id, AbstractPet other, GameTestHelper helper) {
        return pet.getBrain().getMemory(InitMemory.SOCIAL_COOLDOWNS.get())
            .orElse(SocialCooldowns.NONE)
            .coolingDown(id, other.getUUID(), helper.getLevel().getGameTime());
    }
}
