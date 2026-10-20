package com.dwinovo.chiikawa.entity.brain.errand.impl;

import com.dwinovo.chiikawa.entity.brain.errand.Check;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandTunings;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.step.LegacyActivityStep;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.init.InitActivity;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.music.MusicStopReason;
import com.dwinovo.chiikawa.music.ServerMusicSystem;
import com.dwinovo.chiikawa.task.PetWorkCounters;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

/**
 * Performs the song selected on the held music box: once per selection, or over and over
 * while the pet carries a street performance slip. A continuous playback mode of the box
 * keeps the errand available after each song, so it goes on for as long as the pet has
 * nothing better to do.
 *
 * <p>The pet remembers which selection it last started. That memory is forgotten
 * whenever the musician cannot perform (not free, or no longer holding the music
 * box), so coming back plays the song again, as in 0.0.9.
 */
public final class PlayMusicErrand extends LegacyErrand<Plan, ErrandTunings.PlayMusic> {
    public PlayMusicErrand() {
        super(ErrandIds.PLAY_MUSIC, ErrandCategory.WORK, ErrandTunings.PlayMusic.CODEC, () -> InitActivity.MUSICIAN_PLAY.get());
    }

    @Override
    protected Check canStart(PetView view, ErrandTunings.PlayMusic tuning) {
        return view.hasPlayableSelection() ? Check.OK : Check.fail("no_new_song");
    }

    @Override
    protected Plan plan(PetView view, ErrandTunings.PlayMusic tuning) {
        return Heading.NOWHERE;
    }

    @Override
    public Check stillWanted(Plan plan, PetView view, ErrandTunings.PlayMusic tuning) {
        return view.playingMusic() || view.hasPlayableSelection() ? Check.OK : Check.fail("song_over");
    }

    @Override
    public Optional<ResourceLocation> workCounter() {
        return Optional.of(PetWorkCounters.PLAY_MUSIC_SECOND);
    }

    /** Ends the performance's music stream when the pet stops performing. */
    @Override
    protected void onStop(LegacyActivityStep.Runtime runtime) {
        ServerMusicSystem.streams(runtime.level().getServer()).stop(runtime.pet(), MusicStopReason.INTERRUPTED);
    }

    @Override
    public Set<MemoryModuleType<?>> forgetWhenUnavailable() {
        return Set.of(InitMemory.MUSICIAN_LAST_MUSIC_SIGNATURE.get(), InitMemory.MUSICIAN_NOW_PLAYING.get());
    }
}
