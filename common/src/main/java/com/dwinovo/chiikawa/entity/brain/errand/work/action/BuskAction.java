package com.dwinovo.chiikawa.entity.brain.errand.work.action;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.brain.errand.Check;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandTunings;
import com.dwinovo.chiikawa.entity.brain.errand.Offer;
import com.dwinovo.chiikawa.entity.brain.errand.Offers;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestSources;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestStore;
import com.dwinovo.chiikawa.entity.brain.errand.run.Routine;
import com.dwinovo.chiikawa.entity.brain.errand.step.Busk;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.entity.brain.errand.work.Work;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkAction;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkTarget;
import com.dwinovo.chiikawa.music.Playable;
import com.dwinovo.chiikawa.music.ServerMusicSystem;
import com.mojang.serialization.Codec;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

/**
 * Performs the song selected on the held music box: once per selection, or over and over
 * while the pet carries a slip that counts the work. A continuous playback mode of the box
 * keeps the work available after each song, so it goes on for as long as the pet has
 * nothing better to do. What the pet plays and for how long is {@link Busk}'s; a song that is over is the
 * errand done, and the next song is the next errand.
 *
 * <p>The pet remembers which selection it last started. That memory is forgotten
 * whenever the musician cannot perform (not free, or no longer holding the music
 * box), so coming back plays the song again, as in 0.0.9.
 *
 * <p>Only a song that would not start rests the errand, for as long as the music settings say
 * ({@link RestSources#MUSIC_RETRY}): a slip or a continuous mode would ask again the very next tick, and stand
 * the pet still for good while the server's streams are all taken. A song that played to its end, or was cut short,
 * leaves nothing to wait for.
 */
public final class BuskAction implements WorkAction<BuskAction.Performance, ErrandTunings.PlayMusic> {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "busk");

    /**
     * What the pet has set out to play.
     *
     * @param song the selection of the music box and the mode it is in, as they were when the pet decided
     */
    public record Performance(Playable song) implements Plan {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public Codec<ErrandTunings.PlayMusic> tuningCodec() {
        return ErrandTunings.PlayMusic.CODEC;
    }

    @Override
    public Optional<String> refuses(Optional<WorkTarget> target) {
        return target.map(ignored -> "busk takes no target");
    }

    @Override
    public Offers<Performance> offers(Work<Performance, ErrandTunings.PlayMusic> work, PetView view,
            ErrandTunings.PlayMusic tuning) {
        return view.playable(work.countedBy(view))
            .map(song -> Offers.of(Offer.of(new Performance(song))))
            .orElseGet(() -> Offers.none(Check.fail("no_new_song")));
    }

    @Override
    public Routine routine(Work<Performance, ErrandTunings.PlayMusic> work, Performance plan,
            ErrandSettings<ErrandTunings.PlayMusic> settings) {
        return Routine.single(new Busk(plan.song(), settings.tuning().noteParticleIntervalTicks(), work.counter()));
    }

    @Override
    public Set<MemoryModuleType<?>> forgetWhenUnavailable() {
        return Busk.progress();
    }

    @Override
    public RestStore.Kind restStore() {
        return RestStore.musicRetry();
    }

    @Override
    public Map<ResourceLocation, RestSources.Resolver> restSources() {
        return Map.of(RestSources.MUSIC_RETRY, context -> ServerMusicSystem.config().retryTicks());
    }
}
