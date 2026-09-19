package com.dwinovo.chiikawa.entity.brain.intent.impl;

import com.dwinovo.chiikawa.entity.brain.intent.IntentCategory;
import com.dwinovo.chiikawa.entity.brain.intent.IntentCheck;
import com.dwinovo.chiikawa.entity.brain.intent.IntentContext;
import com.dwinovo.chiikawa.entity.brain.intent.IntentRuntime;
import com.dwinovo.chiikawa.entity.brain.intent.PetIntent;
import com.dwinovo.chiikawa.entity.brain.intent.PetIntents;
import com.dwinovo.chiikawa.init.InitActivity;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.music.MusicStopReason;
import com.dwinovo.chiikawa.music.ServerMusicSystem;
import com.dwinovo.chiikawa.task.PetWorkCounters;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.schedule.Activity;

/**
 * Performs the song selected on the held music box: once per selection, or over and over
 * while the pet carries a street performance slip.
 *
 * <p>The pet remembers which selection it last started. That memory is forgotten
 * whenever the musician cannot perform (not free, or no longer holding the music
 * box), so coming back plays the song again, as in 0.0.9.
 */
public final class PlayMusicIntent implements PetIntent {
    private static final float SCORE = 0.6F;

    @Override
    public ResourceLocation id() {
        return PetIntents.PLAY_MUSIC;
    }

    @Override
    public IntentCategory category() {
        return IntentCategory.WORK;
    }

    @Override
    public Activity activity() {
        return InitActivity.MUSICIAN_PLAY.get();
    }

    @Override
    public IntentCheck canRun(IntentContext ctx) {
        return ctx.hasPlayableSelection() ? IntentCheck.OK : IntentCheck.fail("no_new_song");
    }

    @Override
    public IntentCheck canContinue(IntentContext ctx) {
        return ctx.playingMusic() || ctx.hasPlayableSelection() ? IntentCheck.OK : IntentCheck.fail("song_over");
    }

    @Override
    public float score(IntentContext ctx) {
        return SCORE;
    }

    @Override
    public Optional<ResourceLocation> workCounter() {
        return Optional.of(PetWorkCounters.PLAY_MUSIC_SECOND);
    }

    /** Ends the performance's music stream when the pet stops performing. */
    @Override
    public void onStop(IntentRuntime runtime) {
        ServerMusicSystem.streams(runtime.level().getServer()).stop(runtime.pet(), MusicStopReason.INTERRUPTED);
    }

    @Override
    public Set<MemoryModuleType<?>> forgetWhenUnavailable() {
        return Set.of(InitMemory.MUSICIAN_LAST_MUSIC_SIGNATURE.get());
    }
}
