package com.dwinovo.chiikawa.entity.brain.task.musician;

import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetMode;
import com.dwinovo.chiikawa.entity.brain.PetCommand;
import com.dwinovo.chiikawa.entity.impl.HachiwarePet;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.init.InitRegistry;
import com.dwinovo.chiikawa.music.ChiikawaMusicConfig;
import com.dwinovo.chiikawa.music.MusicBoxSelection;
import com.dwinovo.chiikawa.music.MusicStopReason;
import com.dwinovo.chiikawa.music.ServerMusicSystem;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;

/** Runs Hachiware's guitar animation while the musician job owns a music stream session. */
public class PlayMusicBehavior extends Behavior<AbstractPet> {
    private static final int MAX_DURATION_TICKS = ChiikawaMusicConfig.DEFAULT.maxTrackSeconds() * 20 + 40;
    private static final int NOTE_PARTICLE_INTERVAL_TICKS = 50;
    private static final Map<MemoryModuleType<?>, MemoryStatus> REQUIRED_MEMORIES = ImmutableMap.of(
        InitMemory.REQUESTED_COMMAND.get(), MemoryStatus.VALUE_PRESENT,
        InitMemory.REQUESTED_MUSIC_TRACK.get(), MemoryStatus.VALUE_PRESENT,
        MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT,
        MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED,
        MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED
    );

    private String activeTrackId = "";

    public PlayMusicBehavior() {
        super(REQUIRED_MEMORIES, MAX_DURATION_TICKS);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, AbstractPet pet) {
        return pet instanceof HachiwarePet
            && pet.isTame()
            && pet.getPetMode() == PetMode.WORK
            && pet.getPetJobId() == InitRegistry.MUSICIAN_ID
            && pet.getBrain().getMemory(InitMemory.REQUESTED_COMMAND.get()).orElse(null) == PetCommand.PLAY_MUSIC
            && requestedTrack(pet).filter(trackId -> selectedTrack(pet).filter(trackId::equals).isPresent()).isPresent();
    }

    @Override
    protected void start(ServerLevel level, AbstractPet pet, long gameTime) {
        activeTrackId = requestedTrack(pet).orElse("");
        pet.getBrain().eraseMemory(InitMemory.REQUESTED_COMMAND.get());
        pet.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        pet.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        pet.getNavigation().stop();
        lookAtOwner(pet);

        if (activeTrackId.isBlank()
                || ServerMusicSystem.streams(level.getServer()).start(pet, activeTrackId).isEmpty()) {
            clearRequest(pet);
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
    }

    @Override
    protected boolean canStillUse(ServerLevel level, AbstractPet pet, long gameTime) {
        return pet instanceof HachiwarePet
            && pet.getActivity() == PetActivity.PLAY_GUITAR
            && pet.getPetMode() == PetMode.WORK
            && pet.getPetJobId() == InitRegistry.MUSICIAN_ID
            && !pet.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)
            && selectedTrack(pet).filter(activeTrackId::equals).isPresent()
            && ServerMusicSystem.streams(level.getServer()).isPlaying(pet, activeTrackId);
    }

    @Override
    protected void stop(ServerLevel level, AbstractPet pet, long gameTime) {
        clearRequest(pet);
        if (!activeTrackId.isBlank()) {
            ServerMusicSystem.streams(level.getServer()).stop(pet, MusicStopReason.INTERRUPTED);
        }
        activeTrackId = "";
        if (pet.getActivity() == PetActivity.PLAY_GUITAR) {
            pet.setActivity(PetActivity.NONE);
        }
    }

    private static java.util.Optional<String> requestedTrack(AbstractPet pet) {
        return pet.getBrain().getMemory(InitMemory.REQUESTED_MUSIC_TRACK.get()).filter(trackId -> !trackId.isBlank());
    }

    private static java.util.Optional<String> selectedTrack(AbstractPet pet) {
        ItemStack stack = pet.getMainHandItem();
        if (!stack.is(InitItems.MUSIC_BOX.get())) {
            return java.util.Optional.empty();
        }
        MusicBoxSelection selection = MusicBoxSelection.get(stack);
        if (selection == null || selection.trackId().isBlank()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(selection.trackId());
    }

    private static void clearRequest(AbstractPet pet) {
        pet.getBrain().eraseMemory(InitMemory.REQUESTED_COMMAND.get());
        pet.getBrain().eraseMemory(InitMemory.REQUESTED_MUSIC_TRACK.get());
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
