package com.dwinovo.chiikawa.entity.brain.task.musician;

import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.music.ChiikawaMusicConfig;
import com.dwinovo.chiikawa.music.MusicBoxSelection;
import com.dwinovo.chiikawa.music.ServerMusicSystem;
import com.dwinovo.chiikawa.task.PetWorkCounters;
import com.dwinovo.chiikawa.task.TaskTracker;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;

/**
 * Starts a music stream for the music box selection the pet would play (see
 * {@link #playableSelection}) and runs Hachiware's guitar animation while the stream
 * lasts, reporting each second of it as work. The stream itself is ended by the stream
 * manager when the song finishes, by a newer song replacing it, or by the
 * {@code play_music} intent when the pet stops performing.
 */
public class PlayMusicBehavior extends Behavior<AbstractPet> {
    private static final int MAX_DURATION_TICKS = ChiikawaMusicConfig.DEFAULT.maxTrackSeconds() * 20 + 40;
    private static final int NOTE_PARTICLE_INTERVAL_TICKS = 50;
    private static final int TICKS_PER_SECOND = 20;
    private static final Map<MemoryModuleType<?>, MemoryStatus> REQUIRED_MEMORIES = ImmutableMap.of(
        InitMemory.MUSICIAN_LAST_MUSIC_SIGNATURE.get(), MemoryStatus.REGISTERED,
        MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED,
        MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED
    );

    private String activeTrackId = "";
    private int ticksPlayed;

    public PlayMusicBehavior() {
        super(REQUIRED_MEMORIES, MAX_DURATION_TICKS);
    }

    /**
     * A selection plays once, unless the pet carries a street performance slip: then it
     * keeps playing it until the slip is paid.
     *
     * @param pet the pet
     * @return the held music box selection, if the pet would play it now
     */
    public static Optional<MusicBoxSelection> playableSelection(AbstractPet pet) {
        boolean performing = pet.getTask()
            .filter(task -> task.counter().equals(PetWorkCounters.PLAY_MUSIC_SECOND))
            .isPresent();
        Optional<String> lastPlayed = pet.getBrain().getMemory(InitMemory.MUSICIAN_LAST_MUSIC_SIGNATURE.get());
        return selection(pet).filter(selection -> performing || lastPlayed.filter(selection.signature()::equals).isEmpty());
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, AbstractPet pet) {
        return playableSelection(pet).isPresent();
    }

    @Override
    protected void start(ServerLevel level, AbstractPet pet, long gameTime) {
        MusicBoxSelection selection = playableSelection(pet).orElseThrow();
        ticksPlayed = 0;
        // Remembered even if the stream cannot start, so a failing song is not retried.
        pet.getBrain().setMemory(InitMemory.MUSICIAN_LAST_MUSIC_SIGNATURE.get(), selection.signature());
        activeTrackId = selection.trackId();
        pet.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        pet.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        pet.getNavigation().stop();
        lookAtOwner(pet);

        if (ServerMusicSystem.streams(level.getServer()).start(pet, activeTrackId).isEmpty()) {
            activeTrackId = "";
            pet.setActivity(PetActivity.NONE);
            return;
        }
        pet.setActivity(PetActivity.PLAY_GUITAR);
    }

    @Override
    protected void tick(ServerLevel level, AbstractPet pet, long gameTime) {
        pet.getNavigation().stop();
        pet.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        lookAtOwner(pet);
        spawnMusicNote(level, pet, gameTime);
        if (++ticksPlayed % TICKS_PER_SECOND == 0) {
            TaskTracker.advance(pet, PetWorkCounters.PLAY_MUSIC_SECOND, 1);
        }
    }

    @Override
    protected boolean canStillUse(ServerLevel level, AbstractPet pet, long gameTime) {
        return pet.getActivity() == PetActivity.PLAY_GUITAR
            && selection(pet).filter(selection -> selection.trackId().equals(activeTrackId)).isPresent()
            && ServerMusicSystem.streams(level.getServer()).isPlaying(pet, activeTrackId);
    }

    @Override
    protected void stop(ServerLevel level, AbstractPet pet, long gameTime) {
        activeTrackId = "";
        if (pet.getActivity() == PetActivity.PLAY_GUITAR) {
            pet.setActivity(PetActivity.NONE);
        }
    }

    private static Optional<MusicBoxSelection> selection(AbstractPet pet) {
        ItemStack stack = pet.getMainHandItem();
        if (!stack.is(InitItems.MUSIC_BOX.get())) {
            return Optional.empty();
        }
        MusicBoxSelection selection = MusicBoxSelection.get(stack);
        if (selection == null || selection.trackId().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(selection);
    }

    private static void lookAtOwner(AbstractPet pet) {
        LivingEntity owner = pet.getOwner();
        if (owner != null) {
            pet.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(owner, true));
        }
    }

    private static void spawnMusicNote(ServerLevel level, AbstractPet pet, long gameTime) {
        if ((gameTime + pet.getId()) % NOTE_PARTICLE_INTERVAL_TICKS != 0) {
            return;
        }
        double x = pet.getX() + (pet.getRandom().nextDouble() - 0.5D) * 0.45D;
        double y = pet.getY() + pet.getBbHeight() + 0.15D + pet.getRandom().nextDouble() * 0.2D;
        double z = pet.getZ() + (pet.getRandom().nextDouble() - 0.5D) * 0.45D;
        double noteColor = pet.getRandom().nextInt(24) / 24.0D;
        level.sendParticles(ParticleTypes.NOTE, x, y, z, 0, noteColor, 0.0D, 0.0D, 1.0D);
    }
}
