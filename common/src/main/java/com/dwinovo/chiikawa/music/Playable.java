package com.dwinovo.chiikawa.music;

import com.dwinovo.chiikawa.init.InitItems;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;

/**
 * A song a musician would start now: the selection on its music box, in the mode the box is set to.
 *
 * <p>In the default mode a selection plays once, unless the pet carries a street performance slip: then it keeps
 * playing it until the slip is paid. In a continuous mode it is playable for as long as the selected track is ready.
 * Which track plays is {@link PlaybackMode#nextTrack}'s to say: the selected one first, then, in a continuous mode,
 * whatever follows the track the pet last started. A new selection or a new mode on the box starts over from the
 * selected track.
 *
 * @param selection what the box selects
 * @param mode the mode the box is set to
 * @param onSlip whether the pet carries a street performance slip, which has it play the song over whatever the mode
 */
public record Playable(MusicBoxSelection selection, PlaybackMode mode, boolean onSlip) {
    /** @return the selection a music box holds, if it is one and has been given a track */
    public static Optional<MusicBoxSelection> held(ItemStack stack) {
        if (!stack.is(InitItems.MUSIC_BOX.get())) {
            return Optional.empty();
        }
        MusicBoxSelection selection = MusicBoxSelection.get(stack);
        return selection == null || selection.trackId().isBlank() ? Optional.empty() : Optional.of(selection);
    }

    /** Names a selection together with the mode it is played in: either changing starts over. */
    public static String signature(MusicBoxSelection selection, PlaybackMode mode) {
        return selection.signature() + ":" + mode.name();
    }

    /**
     * @param held what the pet's music box selects, if it holds one
     * @param mode the mode the box is set to
     * @param lastStarted the signature of the song the pet last started
     * @param onSlip whether the pet carries a street performance slip
     * @param ready whether a track is ready to play
     * @return the song the pet would play on from what it holds now, if there is one
     */
    public static Optional<Playable> of(Optional<MusicBoxSelection> held, PlaybackMode mode, Optional<String> lastStarted,
            boolean onSlip, Predicate<String> ready) {
        return held.map(selection -> new Playable(selection, mode, onSlip)).filter(song -> mode.continuous()
            ? ready.test(song.selection.trackId())
            : onSlip || lastStarted.filter(song.signature()::equals).isEmpty());
    }

    /** @return the signature of this song, see {@link #signature(MusicBoxSelection, PlaybackMode)} */
    public String signature() {
        return signature(selection, mode);
    }

    /** @return the mode this song plays in: a street performance slip repeats it whatever the mode */
    public PlaybackMode playing() {
        return mode == PlaybackMode.ONCE && onSlip ? PlaybackMode.REPEAT_ONE : mode;
    }
}
