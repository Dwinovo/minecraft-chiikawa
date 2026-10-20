package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.addSong;
import static com.dwinovo.chiikawa.gametest.GameTestKit.owned;
import static com.dwinovo.chiikawa.gametest.GameTestKit.quietYard;
import static com.dwinovo.chiikawa.gametest.GameTestKit.readySong;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.PetMind;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.Message;
import com.dwinovo.chiikawa.init.InitDataComponents;
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.music.MusicBoxSelection;
import com.dwinovo.chiikawa.music.PlaybackMode;
import com.dwinovo.chiikawa.music.ServerMusicLibrary;
import com.dwinovo.chiikawa.music.ServerMusicSystem;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What a musician does when a song of its music box ends. The songs are two short ones of
 * silence written into the server's music folder the way a player adds one; the stream
 * runs on the wall clock, so each takes its couple of real seconds to end.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MusicGameTests {
    private static final String BATCH = "chiikawa_music";
    private static final String FULL_BATCH = "chiikawa_music_full";
    /**
     * A song ends by the wall clock while a test server ticks as fast as it can, so a song of
     * a second or two takes thousands of ticks to end; and importing runs off the server
     * thread in its own time, which the server does not wait for either.
     */
    private static final int CASE_TICKS = 100000;
    private static final int STAND = 2;
    /** Sort one after the other in the catalog, with nothing of the cases' between them. */
    private static final String FIRST = "chiikawa_gametest_mode_a";
    private static final String SECOND = "chiikawa_gametest_mode_b";
    private static final int SONG_SECONDS = 1;
    /** Longer than the pause between two songs, which is a few ticks of the brain choosing again. */
    private static final int QUIET_TICKS = 100;
    /** How long an interrupt is allowed to take: the tick it is told, and the one after. */
    private static final int INTERRUPT_TICKS = 2;

    /** In order: the song after the selected one plays on its own once the selected one ends. */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = CASE_TICKS)
    public static void a_musician_repeating_all_goes_on_to_the_next_song(GameTestHelper helper) {
        quietYard(helper, NOON);
        ServerMusicLibrary library = ServerMusicSystem.library(helper.getLevel().getServer());
        addSong(library, FIRST, SONG_SECONDS);
        addSong(library, SECOND, SONG_SECONDS);
        AtomicReference<AbstractPet> hachiware = new AtomicReference<>();

        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(
                readySong(library, FIRST).isPresent() && readySong(library, SECOND).isPresent(),
                "the songs were never imported"))
            .thenExecute(() -> hachiware.set(busker(helper, library, PlaybackMode.REPEAT_ALL)))
            .thenWaitUntil(() -> helper.assertTrue(nowPlaying(hachiware.get(), library, FIRST),
                "Hachiware never started the selected song"))
            .thenWaitUntil(() -> helper.assertTrue(nowPlaying(hachiware.get(), library, SECOND)
                    && hachiware.get().getActivity() == PetActivity.PLAY_GUITAR,
                "Hachiware never went on to the next song"))
            .thenSucceed();
    }

    /** Once: the selected song plays, and then Hachiware is done, as it always was. */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = CASE_TICKS)
    public static void a_musician_playing_once_stops_after_the_song(GameTestHelper helper) {
        quietYard(helper, NOON);
        ServerMusicLibrary library = ServerMusicSystem.library(helper.getLevel().getServer());
        addSong(library, FIRST, SONG_SECONDS);
        addSong(library, SECOND, SONG_SECONDS);
        AtomicReference<AbstractPet> hachiware = new AtomicReference<>();

        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(
                readySong(library, FIRST).isPresent() && readySong(library, SECOND).isPresent(),
                "the songs were never imported"))
            .thenExecute(() -> hachiware.set(busker(helper, library, PlaybackMode.ONCE)))
            .thenWaitUntil(() -> helper.assertTrue(hachiware.get().getActivity() == PetActivity.PLAY_GUITAR,
                "Hachiware never started playing"))
            .thenWaitUntil(() -> helper.assertTrue(hachiware.get().getActivity() != PetActivity.PLAY_GUITAR,
                "the song never ended"))
            .thenIdle(QUIET_TICKS)
            .thenExecute(() -> {
                helper.assertTrue(hachiware.get().getActivity() != PetActivity.PLAY_GUITAR,
                    "Hachiware played on after a song in the once mode");
                helper.assertTrue(nowPlaying(hachiware.get(), library, FIRST),
                    "Hachiware played another song in the once mode");
            })
            .thenSucceed();
    }

    /** Taken somewhere in the middle of a song, the busker leaves no stream of it going: not for the next tick or the one after. */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = CASE_TICKS)
    public static void a_busker_taken_away_mid_track_has_no_stream_left(GameTestHelper helper) {
        quietYard(helper, NOON);
        ServerMusicLibrary library = ServerMusicSystem.library(helper.getLevel().getServer());
        addSong(library, FIRST, SONG_SECONDS);
        AtomicReference<AbstractPet> hachiware = new AtomicReference<>();

        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(readySong(library, FIRST).isPresent(), "the song was never imported"))
            .thenExecute(() -> hachiware.set(busker(helper, library, PlaybackMode.ONCE)))
            .thenWaitUntil(() -> helper.assertTrue(streaming(helper, hachiware.get(), library), "Hachiware never started the song"))
            .thenExecute(() -> PetMind.post(hachiware.get(), new Message.Moved("test")))
            .thenIdle(INTERRUPT_TICKS)
            .thenExecute(() -> {
                helper.assertTrue(!streaming(helper, hachiware.get(), library), "the song was still being streamed for the pet");
                helper.assertTrue(hachiware.get().getActivity() == PetActivity.NONE, "the pet was still posed as a busker");
            })
            .thenSucceed();
    }

    /**
     * A busker that puts its music box away stops at once, and forgets what it played: it starts the song over when it
     * has the box again.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = CASE_TICKS)
    public static void a_musician_without_its_box_stops_and_forgets_what_it_played(GameTestHelper helper) {
        quietYard(helper, NOON);
        ServerMusicLibrary library = ServerMusicSystem.library(helper.getLevel().getServer());
        addSong(library, FIRST, SONG_SECONDS);
        AtomicReference<AbstractPet> hachiware = new AtomicReference<>();

        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(readySong(library, FIRST).isPresent(), "the song was never imported"))
            .thenExecute(() -> hachiware.set(busker(helper, library, PlaybackMode.ONCE)))
            .thenWaitUntil(() -> helper.assertTrue(streaming(helper, hachiware.get(), library), "Hachiware never started the song"))
            .thenExecute(() -> hachiware.get().setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY))
            .thenWaitUntil(() -> {
                helper.assertTrue(!streaming(helper, hachiware.get(), library), "the music went on without the box");
                helper.assertTrue(!hachiware.get().getBrain().hasMemoryValue(InitMemory.MUSICIAN_NOW_PLAYING.get())
                        && !hachiware.get().getBrain().hasMemoryValue(InitMemory.MUSICIAN_LAST_MUSIC_SIGNATURE.get()),
                    "the pet still remembered what it played");
            })
            .thenSucceed();
    }

    /**
     * With every stream the server allows taken, the song will not start: the musician gives it up, waits as long as the
     * music settings say before it asks again, and is not posed as playing meanwhile. Alone in its batch: the streams are the
     * server's, and the other cases' buskers would be taking them.
     */
    @GameTest(template = "floor16", batch = FULL_BATCH, timeoutTicks = CASE_TICKS)
    public static void a_song_that_will_not_start_rests_the_musician_for_the_retry(GameTestHelper helper) {
        quietYard(helper, NOON);
        ServerMusicLibrary library = ServerMusicSystem.library(helper.getLevel().getServer());
        addSong(library, FIRST, SONG_SECONDS);
        AtomicReference<AbstractPet> hachiware = new AtomicReference<>();
        int retry = library.config().retryTicks();

        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(readySong(library, FIRST).isPresent(), "the song was never imported"))
            .thenExecute(() -> {
                for (int taken = 0; taken < library.config().maxConcurrentSessions(); taken++) {
                    AbstractPet other = GameTestKit.wild(helper, InitEntity.CHIIKAWA_PET.get(), new BlockPos(2 + taken, STAND, 2));
                    helper.assertTrue(ServerMusicSystem.streams(helper.getLevel().getServer())
                        .start(other, readySong(library, FIRST).orElseThrow().trackId()).isPresent(), "a stream was not free to take");
                }
                hachiware.set(busker(helper, library, PlaybackMode.ONCE));
            })
            .thenWaitUntil(() -> helper.assertTrue(hachiware.get().getBrain().hasMemoryValue(InitMemory.MUSICIAN_RESTING.get()),
                "Hachiware never gave up the song"))
            .thenExecute(() -> {
                long left = hachiware.get().getBrain().getTimeUntilExpiry(InitMemory.MUSICIAN_RESTING.get());
                helper.assertTrue(left > 0 && left <= retry, "Hachiware rests for " + left + " ticks, not up to the " + retry);
                helper.assertTrue(hachiware.get().getActivity() == PetActivity.NONE, "the pet was posed as a busker");
                helper.assertTrue(!streaming(helper, hachiware.get(), library), "a stream was started with none free");
            })
            .thenSucceed();
    }

    /** Whether the server is streaming the first song for the pet. */
    private static boolean streaming(GameTestHelper helper, AbstractPet pet, ServerMusicLibrary library) {
        return ServerMusicSystem.streams(helper.getLevel().getServer())
            .isPlaying(pet, readySong(library, FIRST).orElseThrow().trackId());
    }

    /** A free Hachiware with a music box set to the mode, selecting the first of the songs. */
    private static AbstractPet busker(GameTestHelper helper, ServerMusicLibrary library, PlaybackMode mode) {
        ItemStack box = new ItemStack(InitItems.MUSIC_BOX.get());
        box.set(InitDataComponents.MUSIC_BOX_SELECTION.get(),
            new MusicBoxSelection(readySong(library, FIRST).orElseThrow().trackId(), FIRST, 1));
        box.set(InitDataComponents.MUSIC_BOX_MODE.get(), mode);
        AbstractPet busker = owned(helper, InitEntity.HACHIWARE_PET.get(), new BlockPos(8, STAND, 8));
        busker.setItemSlot(EquipmentSlot.MAINHAND, box);
        return busker;
    }

    /** Whether the track the pet last started is the song of that title. */
    private static boolean nowPlaying(AbstractPet pet, ServerMusicLibrary library, String title) {
        return pet.getBrain().getMemory(InitMemory.MUSICIAN_NOW_PLAYING.get())
            .filter(readySong(library, title).orElseThrow().trackId()::equals)
            .isPresent();
    }
}
