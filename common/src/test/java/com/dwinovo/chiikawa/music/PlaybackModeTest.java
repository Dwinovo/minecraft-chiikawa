package com.dwinovo.chiikawa.music;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PlaybackModeTest {
    private static MusicTrackView track(String id, MusicTrackStatus status) {
        return new MusicTrackView(id, id, 100, status, "");
    }

    private static MusicTrackView ready(String id) {
        return track(id, MusicTrackStatus.READY);
    }

    private static final List<MusicTrackView> ABC = List.of(ready("a"), ready("b"), ready("c"));

    @Test
    void nothingPlayedYetStartsFromTheSelectedTrackInEveryMode() {
        for (PlaybackMode mode : PlaybackMode.values()) {
            assertEquals(Optional.of("b"), mode.nextTrack(ABC, "b", null, bound -> 0), mode.name());
        }
    }

    @Test
    void onceStopsAfterTheFirstSong() {
        assertEquals(Optional.empty(), PlaybackMode.ONCE.nextTrack(ABC, "a", "a", bound -> 0));
    }

    @Test
    void repeatAllGoesOnAndWrapsFromTheLastToTheFirst() {
        assertEquals(Optional.of("b"), PlaybackMode.REPEAT_ALL.nextTrack(ABC, "a", "a", bound -> 0));
        assertEquals(Optional.of("c"), PlaybackMode.REPEAT_ALL.nextTrack(ABC, "a", "b", bound -> 0));
        assertEquals(Optional.of("a"), PlaybackMode.REPEAT_ALL.nextTrack(ABC, "a", "c", bound -> 0));
    }

    @Test
    void repeatAllSkipsTracksThatAreNotReady() {
        List<MusicTrackView> catalog = List.of(ready("a"), track("b", MusicTrackStatus.IMPORTING),
            track("c", MusicTrackStatus.FAILED), ready("d"));

        assertEquals(Optional.of("d"), PlaybackMode.REPEAT_ALL.nextTrack(catalog, "a", "a", bound -> 0));
        assertEquals(Optional.of("a"), PlaybackMode.REPEAT_ALL.nextTrack(catalog, "a", "d", bound -> 0));
    }

    @Test
    void repeatAllOnASingleReadyTrackPlaysItAgain() {
        List<MusicTrackView> catalog = List.of(track("a", MusicTrackStatus.FAILED), ready("b"));

        assertEquals(Optional.of("b"), PlaybackMode.REPEAT_ALL.nextTrack(catalog, "b", "b", bound -> 0));
    }

    @Test
    void repeatAllStartsOverFromTheSelectedTrackWhenTheFinishedOneLeftTheLibrary() {
        assertEquals(Optional.of("b"), PlaybackMode.REPEAT_ALL.nextTrack(ABC, "b", "gone", bound -> 0));
    }

    @Test
    void shuffleNeverRepeatsTheFinishedTrackWhenThereIsAChoice() {
        Set<String> seen = new HashSet<>();
        for (int roll = 0; roll < 2; roll++) {
            int fixed = roll;
            seen.add(PlaybackMode.SHUFFLE.nextTrack(ABC, "a", "b", bound -> Math.min(fixed, bound - 1)).orElseThrow());
        }
        assertEquals(Set.of("a", "c"), seen);
    }

    @Test
    void shuffleOnlyOffersTheChoicesItIsAskedToPickFrom() {
        int[] asked = new int[1];
        PlaybackMode.SHUFFLE.nextTrack(ABC, "a", "b", bound -> {
            asked[0] = bound;
            return 0;
        });
        assertEquals(2, asked[0]);
    }

    @Test
    void shuffleSkipsTracksThatAreNotReady() {
        List<MusicTrackView> catalog = List.of(ready("a"), track("b", MusicTrackStatus.IMPORTING), ready("c"));

        for (int roll = 0; roll < 2; roll++) {
            int fixed = roll;
            assertTrue(Set.of("a", "c").contains(
                PlaybackMode.SHUFFLE.nextTrack(catalog, "a", "c", bound -> Math.min(fixed, bound - 1)).orElseThrow()));
        }
        assertEquals(Optional.of("a"), PlaybackMode.SHUFFLE.nextTrack(catalog, "a", "c", bound -> 0));
    }

    @Test
    void shuffleOnASingleTrackLibraryPlaysItAgain() {
        List<MusicTrackView> catalog = List.of(ready("a"));

        assertEquals(Optional.of("a"), PlaybackMode.SHUFFLE.nextTrack(catalog, "a", "a", bound -> 0));
    }

    @Test
    void repeatOnePlaysTheSelectedTrackAgain() {
        assertEquals(Optional.of("b"), PlaybackMode.REPEAT_ONE.nextTrack(ABC, "b", "c", bound -> 0));
    }

    @Test
    void nothingPlaysWhenTheSelectedTrackIsGoneOrNotReady() {
        List<MusicTrackView> catalog = List.of(ready("a"), track("b", MusicTrackStatus.IMPORTING));

        for (PlaybackMode mode : PlaybackMode.values()) {
            assertEquals(Optional.empty(), mode.nextTrack(catalog, "missing", null, bound -> 0), mode.name());
            assertEquals(Optional.empty(), mode.nextTrack(catalog, "b", "a", bound -> 0), mode.name());
            assertEquals(Optional.empty(), mode.nextTrack(List.of(), "a", null, bound -> 0), mode.name());
        }
    }

    @Test
    void cyclingVisitsEveryModeAndComesBackToOnce() {
        assertEquals(PlaybackMode.REPEAT_ALL, PlaybackMode.ONCE.cycle());
        assertEquals(PlaybackMode.SHUFFLE, PlaybackMode.REPEAT_ALL.cycle());
        assertEquals(PlaybackMode.REPEAT_ONE, PlaybackMode.SHUFFLE.cycle());
        assertEquals(PlaybackMode.ONCE, PlaybackMode.REPEAT_ONE.cycle());
    }

    @Test
    void codecRoundTripsEveryModeAndReadsAnUnknownOneAsOnce() {
        for (PlaybackMode mode : PlaybackMode.values()) {
            assertEquals(mode, PlaybackMode.fromId(mode.ordinal()));
        }
        assertEquals(PlaybackMode.ONCE, PlaybackMode.fromId(99));
        assertEquals(PlaybackMode.ONCE, PlaybackMode.fromId(-1));
    }
}
