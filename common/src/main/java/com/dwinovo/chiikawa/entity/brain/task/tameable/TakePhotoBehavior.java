package com.dwinovo.chiikawa.entity.brain.task.tameable;

import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.camera.CameraSettings;
import com.dwinovo.chiikawa.camera.PhotoDelivery;
import com.dwinovo.chiikawa.camera.PhotoOutlook;
import com.dwinovo.chiikawa.camera.PhotoScout;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetGesture;
import com.dwinovo.chiikawa.entity.brain.intent.IntentSelector;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.photo.PhotoSystem;
import com.dwinovo.chiikawa.photo.PhotoUploads;
import com.dwinovo.chiikawa.voice.PetSpeech;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

/**
 * Takes a photo: goes to a few blocks from who it is of, turns to them, holds the camera
 * up for the camera's {@code aim_ticks}, and presses the shutter. The picture itself is
 * drawn by the owner's game and comes back a moment later; what becomes of the photo then
 * is {@link PhotoDelivery}'s, so the pet is free of it as soon as the shutter has gone and
 * has held the pose a little while after.
 *
 * <p>A photo is posed or candid, rolled each time (see {@link CameraSettings#posed}). Posed,
 * Hachiware calls out, and whoever is being photographed, if it is a pet, turns to look at the
 * camera and strikes a pose of its own, with a word to go with it. Candid, Hachiware says
 * nothing and a pet being photographed is left entirely alone: it goes on with what it was
 * doing, which is what the photo is of.
 */
public class TakePhotoBehavior extends Behavior<AbstractPet> {
    private static final float SPEED = 0.7F;
    /** How near the walk gets to who is being photographed: a few blocks, as a photographer stands. */
    private static final int STAND_BACK = 4;
    private static final double STAND_BACK_SQR = (STAND_BACK + 1.0) * (STAND_BACK + 1.0);
    /** How long the pose is held after the shutter, so the camera is not put away before the picture is drawn. */
    private static final int HOLD_TICKS = 20;
    /** How long whoever is photographed keeps looking at the camera. */
    private static final int LOOK_BACK_TICKS = 60;
    /** How far a view is looked at, for the pet to turn its head towards. */
    private static final double VIEW_DISTANCE = 16.0;
    /** Long enough to cross a garden, aim and hold; a pet that cannot get there gives up. */
    private static final int GIVE_UP_TICKS = 1200;
    /** The longest a pet walks to somebody before it decides they are not worth the walk. */
    private static final int MAX_WALK_TICKS = 400;
    /** How long it has to be unable to find a way before it gives up on one. */
    private static final int NO_WAY_TICKS = 100;
    private static final String POSE = "photographed";

    private enum Stage {
        WALKING,
        AIMING,
        HOLDING
    }

    private Stage stage = Stage.WALKING;
    private PhotoScout.Shot shot;
    private CameraSettings camera;
    private long since;
    private boolean finished;
    /** Whether the owner's game was asked for a picture. */
    private boolean requested;
    /** Whether it went without taking the photo: it had nobody to take one of, could not get to them, or lost them. */
    private boolean gaveUp;

    public TakePhotoBehavior() {
        super(ImmutableMap.of(
            InitMemory.PHOTO_COOLDOWN.get(), MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED,
            MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED
        ), GIVE_UP_TICKS);
    }

    /** Able to, and with the photo it was sent to take: the selector chose it, and admitted going there. */
    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, AbstractPet pet) {
        return PhotoScout.outlook(pet).canContinue().ok() && pet.getBrain().hasMemoryValue(InitMemory.PHOTO_PLAN.get());
    }

    @Override
    protected void start(ServerLevel level, AbstractPet pet, long gameTime) {
        finished = false;
        requested = false;
        gaveUp = false;
        stage = Stage.WALKING;
        since = gameTime;
        PhotoOutlook outlook = PhotoScout.outlook(pet);
        camera = outlook.camera().orElseThrow();
        // Who it was sent to, unless they are gone by the time it sets off.
        Optional<PhotoScout.Shot> planned = pet.getBrain().getMemory(InitMemory.PHOTO_PLAN.get())
            .flatMap(plan -> PhotoScout.resolve(level, plan));
        if (planned.isEmpty()) {
            gaveUp = true;
            finished = true;
            return;
        }
        shot = planned.get();
        // Not left over from some other walk: only a path this one could not find counts against it.
        pet.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        shot.target().ifPresentOrElse(
            target -> BehaviorUtils.setWalkAndLookTargetMemories(pet, target, SPEED, STAND_BACK),
            () -> beginAiming(pet, gameTime));
    }

    @Override
    protected boolean canStillUse(ServerLevel level, AbstractPet pet, long gameTime) {
        return !finished;
    }

    @Override
    protected void tick(ServerLevel level, AbstractPet pet, long gameTime) {
        if (shot.target().filter(target -> !target.isAlive()).isPresent()) {
            gaveUp = true;
            finished = true;
            return;
        }
        switch (stage) {
            case WALKING -> {
                if (pet.distanceToSqr(shot.target().orElseThrow()) <= STAND_BACK_SQR) {
                    beginAiming(pet, gameTime);
                } else if (gameTime - since >= MAX_WALK_TICKS || cannotGetThere(pet, gameTime)) {
                    gaveUp = true;
                    finished = true;
                } else if (!pet.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET)) {
                    // The walk is dropped when no path can be found just now, such as in the tick
                    // the pet lands from where it was spawned, and is set off on again.
                    BehaviorUtils.setWalkAndLookTargetMemories(pet, shot.target().orElseThrow(), SPEED, STAND_BACK);
                }
            }
            case AIMING -> {
                face(pet);
                if (gameTime - since >= camera.aimTicks()) {
                    shutter(level, pet, gameTime);
                }
            }
            case HOLDING -> {
                face(pet);
                finished = gameTime - since >= HOLD_TICKS;
            }
        }
    }

    @Override
    protected void stop(ServerLevel level, AbstractPet pet, long gameTime) {
        if (!requested && gaveUp) {
            // Somebody it could not get to, or lost, is somebody it would try again for, tick after
            // tick, were it not to rest.
            pet.getBrain().setMemoryWithExpiry(InitMemory.PHOTO_COOLDOWN.get(), Unit.INSTANCE, camera.retryTicks());
        }
        pet.setActivity(PetActivity.NONE);
        pet.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        pet.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        // Over: the pet picks what to do next now, and not at its next turn, which would find the
        // photo still its activity.
        IntentSelector.requestReevaluate(pet);
    }

    /**
     * Whether the pet has been unable to find a way to who it set off for for some time. A
     * path that fails now and then, over ground still being loaded or round a corner, is
     * not a path that cannot be found.
     */
    private static boolean cannotGetThere(AbstractPet pet, long gameTime) {
        return pet.getBrain().getMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE)
            .filter(blockedSince -> gameTime - blockedSince >= NO_WAY_TICKS).isPresent();
    }

    /** Arrived: stops where it is, turns to who it is photographing and puts the camera up. */
    private void beginAiming(AbstractPet pet, long gameTime) {
        pet.getNavigation().stop();
        pet.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        pet.setActivity(PetActivity.PHOTO);
        stage = Stage.AIMING;
        since = gameTime;
        face(pet);
    }

    /** Looks at who it is photographing, or out at the view. */
    private void face(AbstractPet pet) {
        pet.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, shot.target()
            .<PositionTracker>map(target -> new EntityTracker(target, true))
            .orElseGet(() -> new BlockPosTracker(pet.getEyePosition().add(shot.facing().scale(VIEW_DISTANCE)))));
    }

    /**
     * The shutter: when it is posed, whoever is in the photo turns to the camera and poses, both say
     * something, and the owner's game is asked for the picture.
     */
    private void shutter(ServerLevel level, AbstractPet pet, long gameTime) {
        stage = Stage.HOLDING;
        since = gameTime;
        if (camera.posed(pet.getRandom())) {
            PetSpeech.say(pet, VoiceMoment.PHOTO);
            shot.target().filter(target -> target instanceof AbstractPet).map(target -> (AbstractPet) target)
                .ifPresent(friend -> pose(friend, pet));
        }
        if (!(pet.getOwner() instanceof ServerPlayer owner)) {
            return;
        }
        PhotoSystem.of(level.getServer()).request(owner, pet, camera,
            new PhotoDelivery(pet, camera, shot.kind(), shot.name(), PhotoScout.biomeOf(pet)));
        requested = true;
        // Rests from photos until the picture is back, so it does not take a second while it
        // waits for the first. Set here, as it is asked for, so that what PhotoDelivery says when
        // the picture is back, however soon, has the last word: what the picture then costs, and
        // how long the rest is after.
        pet.getBrain().setMemoryWithExpiry(InitMemory.PHOTO_COOLDOWN.get(), Unit.INSTANCE, PhotoUploads.WAIT_TICKS);
    }

    /** The friend being photographed looks at the camera, strikes its pose and says something. */
    private static void pose(AbstractPet friend, LivingEntity photographer) {
        friend.getBrain().setMemoryWithExpiry(MemoryModuleType.LOOK_TARGET, new EntityTracker(photographer, true),
            LOOK_BACK_TICKS);
        PetGesture.make(friend, POSE);
        PetSpeech.say(friend, VoiceMoment.PHOTOGRAPHED);
    }
}
