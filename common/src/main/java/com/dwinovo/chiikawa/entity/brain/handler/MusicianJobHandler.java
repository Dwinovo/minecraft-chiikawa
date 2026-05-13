package com.dwinovo.chiikawa.entity.brain.handler;

import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetMode;
import com.dwinovo.chiikawa.entity.brain.PetCommand;
import com.dwinovo.chiikawa.entity.brain.task.musician.PlayMusicBehavior;
import com.dwinovo.chiikawa.entity.impl.HachiwarePet;
import com.dwinovo.chiikawa.init.InitActivity;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.init.InitRegistry;
import com.dwinovo.chiikawa.music.MusicBoxSelection;
import com.dwinovo.chiikawa.music.MusicStopReason;
import com.dwinovo.chiikawa.music.ServerMusicSystem;
import com.dwinovo.chiikawa.utils.BrainUtils;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;

public final class MusicianJobHandler {
    private static final long MUSIC_REQUEST_TICKS = 40L;

    private MusicianJobHandler() {
    }

    public static void registerActivities(Brain<AbstractPet> brain) {
        BrainUtils.addActivity(brain, InitActivity.MUSICIAN_PLAY.get(), ImmutableList.of(
            Pair.of(0, new PlayMusicBehavior())
        ));
    }

    public static void tickBrain(AbstractPet pet, Brain<AbstractPet> brain) {
        if (!canWorkAsMusician(pet)) {
            clearMusicState(pet, brain);
            brain.setActiveActivityToFirstValid(ImmutableList.of(Activity.IDLE));
            return;
        }

        Optional<MusicBoxSelection> selection = selectedMusicBox(pet);
        if (selection.isEmpty()) {
            clearMusicState(pet, brain);
            brain.setActiveActivityToFirstValid(ImmutableList.of(Activity.IDLE));
            return;
        }

        boolean playing = pet.getActivity() == PetActivity.PLAY_GUITAR;
        if (!playing) {
            requestSelectionIfChanged(brain, selection.get());
        }

        ImmutableList.Builder<Activity> activities = ImmutableList.builder();
        if (playing || hasPendingMusicRequest(brain)) {
            activities.add(InitActivity.MUSICIAN_PLAY.get());
        }
        activities.add(Activity.IDLE);
        brain.setActiveActivityToFirstValid(activities.build());
    }

    public static void clearMusicState(AbstractPet pet, Brain<AbstractPet> brain) {
        brain.eraseMemory(InitMemory.REQUESTED_COMMAND.get());
        brain.eraseMemory(InitMemory.REQUESTED_MUSIC_TRACK.get());
        brain.eraseMemory(InitMemory.MUSICIAN_LAST_MUSIC_SIGNATURE.get());
        if (pet.level() instanceof ServerLevel serverLevel) {
            ServerMusicSystem.streams(serverLevel.getServer()).stop(pet, MusicStopReason.INTERRUPTED);
        }
        if (pet.getActivity() == PetActivity.PLAY_GUITAR) {
            pet.setActivity(PetActivity.NONE);
        }
    }

    private static boolean canWorkAsMusician(AbstractPet pet) {
        return pet instanceof HachiwarePet
            && pet.isTame()
            && pet.getPetMode() == PetMode.WORK
            && pet.getPetJobId() == InitRegistry.MUSICIAN_ID
            && !pet.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET);
    }

    private static void requestSelectionIfChanged(Brain<AbstractPet> brain, MusicBoxSelection selection) {
        if (selection.trackId().isBlank()) {
            return;
        }
        String signature = selection.signature();
        if (brain.getMemory(InitMemory.MUSICIAN_LAST_MUSIC_SIGNATURE.get()).filter(signature::equals).isPresent()) {
            return;
        }
        brain.setMemoryWithExpiry(InitMemory.REQUESTED_COMMAND.get(), PetCommand.PLAY_MUSIC, MUSIC_REQUEST_TICKS);
        brain.setMemoryWithExpiry(InitMemory.REQUESTED_MUSIC_TRACK.get(), selection.trackId(), MUSIC_REQUEST_TICKS);
        brain.setMemory(InitMemory.MUSICIAN_LAST_MUSIC_SIGNATURE.get(), signature);
    }

    private static boolean hasPendingMusicRequest(Brain<AbstractPet> brain) {
        return brain.getMemory(InitMemory.REQUESTED_COMMAND.get()).orElse(null) == PetCommand.PLAY_MUSIC
            && brain.getMemory(InitMemory.REQUESTED_MUSIC_TRACK.get()).filter(trackId -> !trackId.isBlank()).isPresent();
    }

    private static Optional<MusicBoxSelection> selectedMusicBox(AbstractPet pet) {
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
}
