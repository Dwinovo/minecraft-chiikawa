package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.Message;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepTag;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.music.MusicStopReason;
import com.dwinovo.chiikawa.music.Playable;
import com.dwinovo.chiikawa.music.PlaybackMode;
import com.dwinovo.chiikawa.music.ServerMusicLibrary;
import com.dwinovo.chiikawa.music.ServerMusicStreamManager;
import com.dwinovo.chiikawa.music.ServerMusicSystem;
import com.dwinovo.chiikawa.task.PetWorkCounters;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

/**
 * Starts a music stream for the song the pet is to play and plays it, standing where it is and looking at its owner,
 * reporting each second of it as work. Done when the song is over, which the stream manager tells the pet's mind
 * and the mind tells the step, and when the music box no longer selects what the stream was started for. Gives up
 * if the stream cannot be started. Always interruptible; however it ends, the stream is stopped.
 *
 * <p>Which track plays is the song's to say, see {@link Playable}. The pet remembers the signature of the song it last
 * started, even if the stream cannot start so that a failing song is not tried again, and the track it started, which
 * a continuous mode follows on from. This step is the only one to write either of them.
 */
public final class Busk implements Step {
    private static final int TICKS_PER_SECOND = 20;

    private final Playable song;
    private final int noteInterval;
    /** The track the stream plays; empty while there is none, as long as the step has not started one. */
    private String trackId = "";
    /** Why the stream could not be started, if it could not. */
    private String failure = "";
    private int ticksPlayed;

    /**
     * @param song what to play
     * @param noteInterval how often a note rises from the pet while it plays, in ticks
     */
    public Busk(Playable song, int noteInterval) {
        this.song = song;
        this.noteInterval = noteInterval;
    }

    /** @return the memories that hold what the pet has played, which only make sense while it can play */
    public static Set<MemoryModuleType<?>> progress() {
        return Set.of(InitMemory.MUSICIAN_LAST_MUSIC_SIGNATURE.get(), InitMemory.MUSICIAN_NOW_PLAYING.get());
    }

    @Override
    public void begin(Handle handle) {
        AbstractPet pet = LiveHandle.of(handle).pet();
        Brain<AbstractPet> brain = pet.getBrain();
        ServerLevel level = (ServerLevel) pet.level();
        ServerMusicLibrary library = ServerMusicSystem.library(level.getServer());
        String signature = song.signature();
        // The track that just finished, unless this selection and mode have not played yet.
        String finished = brain.getMemory(InitMemory.MUSICIAN_LAST_MUSIC_SIGNATURE.get())
            .filter(signature::equals)
            .flatMap(ignored -> brain.getMemory(InitMemory.MUSICIAN_NOW_PLAYING.get()))
            .orElse(null);
        Optional<String> next = song.playing().nextTrack(library.catalog(), song.selection().trackId(), finished,
            pet.getRandom()::nextInt);
        // Remembered even if the stream cannot start, so a failing song is not retried.
        brain.setMemory(InitMemory.MUSICIAN_LAST_MUSIC_SIGNATURE.get(), signature);
        next.ifPresent(track -> brain.setMemory(InitMemory.MUSICIAN_NOW_PLAYING.get(), track));
        handle.stopWalking();
        pet.getNavigation().stop();
        lookAtOwner(handle, pet);

        if (next.isEmpty()) {
            failure = "no_track";
        } else if (streams(pet).start(pet, next.get()).isEmpty()) {
            failure = "no_stream";
        } else {
            trackId = next.get();
        }
    }

    @Override
    public StepStatus tick(Handle handle) {
        if (!failure.isEmpty()) {
            return StepStatus.gaveUp(failure);
        }
        AbstractPet pet = LiveHandle.of(handle).pet();
        if (!stillSelected(pet)) {
            return StepStatus.DONE;
        }
        lookAtOwner(handle, pet);
        spawnNote((ServerLevel) pet.level(), pet, handle.now());
        if (++ticksPlayed % TICKS_PER_SECOND == 0) {
            handle.reportWork(PetWorkCounters.PLAY_MUSIC_SECOND, 1);
        }
        return StepStatus.RUNNING;
    }

    /** The stream manager tells the pet that its song has run its course. */
    @Override
    public StepStatus onMessage(Handle handle, Message message) {
        return message instanceof Message.MusicStopped ? StepStatus.DONE : StepStatus.RUNNING;
    }

    @Override
    public void release(Handle handle) {
        if (!trackId.isEmpty()) {
            AbstractPet pet = LiveHandle.of(handle).pet();
            streams(pet).stop(pet, MusicStopReason.INTERRUPTED);
        }
    }

    @Override
    public PetActivity pose() {
        return trackId.isEmpty() ? PetActivity.NONE : PetActivity.PLAY_GUITAR;
    }

    @Override
    public boolean holdsGaze() {
        return true;
    }

    @Override
    public Set<StepTag> tags() {
        return Set.of(StepTag.PLAYING_MUSIC);
    }

    /** Whether the music box still selects, in the mode it is in, what the stream was started for. */
    private boolean stillSelected(AbstractPet pet) {
        return Playable.held(pet.getMainHandItem())
            .map(selection -> Playable.signature(selection, PlaybackMode.get(pet.getMainHandItem())))
            .filter(song.signature()::equals).isPresent();
    }

    private static ServerMusicStreamManager streams(AbstractPet pet) {
        return ServerMusicSystem.streams(pet.level().getServer());
    }

    private static void lookAtOwner(Handle handle, AbstractPet pet) {
        LivingEntity owner = pet.getOwner();
        if (owner != null) {
            handle.lookAt(owner);
        }
    }

    private void spawnNote(ServerLevel level, AbstractPet pet, long gameTime) {
        if ((gameTime + pet.getId()) % noteInterval != 0) {
            return;
        }
        double x = pet.getX() + (pet.getRandom().nextDouble() - 0.5D) * 0.45D;
        double y = pet.getY() + pet.getBbHeight() + 0.15D + pet.getRandom().nextDouble() * 0.2D;
        double z = pet.getZ() + (pet.getRandom().nextDouble() - 0.5D) * 0.45D;
        double noteColor = pet.getRandom().nextInt(24) / 24.0D;
        level.sendParticles(ParticleTypes.NOTE, x, y, z, 0, noteColor, 0.0D, 0.0D, 1.0D);
    }
}
