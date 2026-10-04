package com.dwinovo.chiikawa.music;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.IntUnaryOperator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * What a musician does when a song of the music box ends. The box holds the owner's
 * choice; which song is playing now is the pet's own state, so this only answers
 * "what comes next" from the mode, the catalog, the selected song and the song that
 * just finished — see {@link #nextTrack}.
 */
public enum PlaybackMode {
    /** Stops after the selected song, which is what the music box always did. */
    ONCE,
    /** The next song in the catalog's order, wrapping round to the first. */
    IN_ORDER,
    /** Any other ready song, never the one that just played while there is a choice. */
    SHUFFLE,
    /** The selected song again. */
    REPEAT_ONE;

    /** As saved on an item: the ordinal, which is what the whistle's order is saved with too. */
    private static final String MODE_KEY = "Mode";

    /** The mode a music box is in; a box that was never given one plays once. */
    public static PlaybackMode get(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(MusicBoxSelection.TAG_KEY);
        return tag == null ? ONCE : fromId(tag.getInt(MODE_KEY));
    }

    public static void set(ItemStack stack, PlaybackMode mode) {
        stack.getOrCreateTagElement(MusicBoxSelection.TAG_KEY).putInt(MODE_KEY, mode.ordinal());
    }

    public static PlaybackMode fromId(int id) {
        PlaybackMode[] values = values();
        return id < 0 || id >= values.length ? ONCE : values[id];
    }

    /** The mode the screen's button moves to: once, in order, shuffle, repeat one, once again. */
    public PlaybackMode cycle() {
        PlaybackMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    /** Whether the pet plays on after a song for as long as it has nothing else to do. */
    public boolean continuous() {
        return this != ONCE;
    }

    /** The name the screen writes for this mode. */
    public String translationKey() {
        return "screen.chiikawa.music_box.mode." + name().toLowerCase(Locale.ROOT);
    }

    /**
     * What plays next. A song that is not ready is never picked, and when the selected song
     * is not ready (or gone) nothing plays at all, whatever the mode.
     *
     * @param catalog the library's tracks in catalog order
     * @param selectedId the track the owner selected on the box
     * @param finishedId the track that just finished, or null when nothing of this
     *     selection has played yet, which starts from the selected track
     * @param pick chooses an index in {@code [0, bound)}; only {@link #SHUFFLE} asks
     * @return the id of the track to play, or empty when nothing should
     */
    public Optional<String> nextTrack(List<MusicTrackView> catalog, String selectedId, @Nullable String finishedId,
            IntUnaryOperator pick) {
        boolean selectedReady = catalog.stream()
            .anyMatch(track -> track.trackId().equals(selectedId) && track.status() == MusicTrackStatus.READY);
        if (!selectedReady) {
            return Optional.empty();
        }
        if (finishedId == null || this == REPEAT_ONE) {
            return Optional.of(selectedId);
        }
        return switch (this) {
            case IN_ORDER -> inOrder(catalog, selectedId, finishedId);
            case SHUFFLE -> shuffle(catalog, finishedId, pick);
            default -> Optional.empty();
        };
    }

    private static Optional<String> inOrder(List<MusicTrackView> catalog, String selectedId, String finishedId) {
        int finished = -1;
        for (int i = 0; i < catalog.size(); i++) {
            if (catalog.get(i).trackId().equals(finishedId)) {
                finished = i;
            }
        }
        if (finished < 0) {
            // The finished song left the library: start over from the selected one.
            return Optional.of(selectedId);
        }
        // Up to a full lap, so a library with one ready song plays that one again.
        for (int step = 1; step <= catalog.size(); step++) {
            MusicTrackView candidate = catalog.get((finished + step) % catalog.size());
            if (candidate.status() == MusicTrackStatus.READY) {
                return Optional.of(candidate.trackId());
            }
        }
        return Optional.of(selectedId);
    }

    private static Optional<String> shuffle(List<MusicTrackView> catalog, String finishedId, IntUnaryOperator pick) {
        List<String> ready = catalog.stream()
            .filter(track -> track.status() == MusicTrackStatus.READY)
            .map(MusicTrackView::trackId)
            .toList();
        List<String> others = ready.stream().filter(id -> !id.equals(finishedId)).toList();
        List<String> choices = others.isEmpty() ? ready : others;
        return Optional.of(choices.get(pick.applyAsInt(choices.size())));
    }
}
