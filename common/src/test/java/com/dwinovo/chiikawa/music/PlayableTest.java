package com.dwinovo.chiikawa.music;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Which song a musician would start from the music box it holds. */
class PlayableTest {
    private static final MusicBoxSelection SELECTED = new MusicBoxSelection("a", "A", 1);

    private static Optional<Playable> of(PlaybackMode mode, Optional<String> lastStarted, boolean onSlip, Set<String> ready) {
        return Playable.of(Optional.of(SELECTED), mode, lastStarted, onSlip, ready::contains);
    }

    @Test
    void aBoxWithNothingSelectedPlaysNothing() {
        assertEquals(Optional.empty(), Playable.of(Optional.empty(), PlaybackMode.REPEAT_ALL, Optional.empty(), true, track -> true));
    }

    @Test
    void aSelectionPlaysOnceAndThenNotAgainUntilTheBoxChanges() {
        Playable fresh = of(PlaybackMode.ONCE, Optional.empty(), false, Set.of()).orElseThrow();
        String signature = fresh.signature();

        assertEquals(Optional.empty(), of(PlaybackMode.ONCE, Optional.of(signature), false, Set.of()));
        assertTrue(of(PlaybackMode.ONCE, Optional.of("b:1:ONCE"), false, Set.of()).isPresent(), "another selection starts over");
        assertTrue(of(PlaybackMode.ONCE, Optional.of("a:2:ONCE"), false, Set.of()).isPresent(), "so does a new revision");
    }

    @Test
    void aStreetPerformanceKeepsPlayingTheSongAndRepeatsIt() {
        Playable song = of(PlaybackMode.ONCE, Optional.of("a:1:ONCE"), true, Set.of()).orElseThrow();

        assertEquals(PlaybackMode.REPEAT_ONE, song.playing());
        assertEquals(PlaybackMode.SHUFFLE, of(PlaybackMode.SHUFFLE, Optional.empty(), true, Set.of("a")).orElseThrow().playing());
    }

    @Test
    void aContinuousModeKeepsPlayingForAsLongAsTheSelectedTrackIsReady() {
        String signature = Playable.signature(SELECTED, PlaybackMode.REPEAT_ALL);

        assertTrue(of(PlaybackMode.REPEAT_ALL, Optional.of(signature), false, Set.of("a")).isPresent());
        assertEquals(Optional.empty(), of(PlaybackMode.REPEAT_ALL, Optional.of(signature), false, Set.of("b")));
    }

    @Test
    void theModeIsPartOfTheSignature() {
        assertEquals("a:1:SHUFFLE", Playable.signature(SELECTED, PlaybackMode.SHUFFLE));
    }
}
