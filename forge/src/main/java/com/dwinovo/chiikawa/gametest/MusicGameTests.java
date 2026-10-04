package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.addSong;
import static com.dwinovo.chiikawa.gametest.GameTestKit.owned;
import static com.dwinovo.chiikawa.gametest.GameTestKit.quietYard;
import static com.dwinovo.chiikawa.gametest.GameTestKit.readySong;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.entity.AbstractPet;
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
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.GameTestDontPrefix;

/**
 * What a musician does when a song of its music box ends. The songs are two short ones of
 * silence written into the server's music folder the way a player adds one; the stream
 * runs on the wall clock, so each takes its couple of real seconds to end.
 */
@GameTestHolder(namespace = Constants.MOD_ID)
@GameTestDontPrefix
public final class MusicGameTests {
    private static final String BATCH = "chiikawa_music";
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

    /** A free Hachiware with a music box set to the mode, selecting the first of the songs. */
    private static AbstractPet busker(GameTestHelper helper, ServerMusicLibrary library, PlaybackMode mode) {
        ItemStack box = new ItemStack(InitItems.MUSIC_BOX.get());
        MusicBoxSelection.set(box,
            new MusicBoxSelection(readySong(library, FIRST).orElseThrow().trackId(), FIRST, 1));
        PlaybackMode.set(box, mode);
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
