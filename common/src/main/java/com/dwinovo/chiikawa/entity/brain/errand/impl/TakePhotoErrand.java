package com.dwinovo.chiikawa.entity.brain.errand.impl;

import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.camera.CameraSettings;
import com.dwinovo.chiikawa.camera.PhotoOutlook;
import com.dwinovo.chiikawa.camera.PhotoPlan;
import com.dwinovo.chiikawa.entity.brain.errand.Check;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandTunings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandType;
import com.dwinovo.chiikawa.entity.brain.errand.Offer;
import com.dwinovo.chiikawa.entity.brain.errand.Offers;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.Message;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestSources;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestStore;
import com.dwinovo.chiikawa.entity.brain.errand.run.Routine;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import com.dwinovo.chiikawa.entity.brain.errand.step.Aim;
import com.dwinovo.chiikawa.entity.brain.errand.step.Goal;
import com.dwinovo.chiikawa.entity.brain.errand.step.Hold;
import com.dwinovo.chiikawa.entity.brain.errand.step.Shutter;
import com.dwinovo.chiikawa.entity.brain.errand.step.WalkTo;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.photo.PhotoUploads;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.ToIntFunction;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Hachiware, with a camera and film in its backpack, takes a photo when there is something
 * worth one: its owner in sight, a friend caught in a little scene, a view at sunrise or
 * sunset or in a biome it has not photographed yet. What it is that makes a moment worth
 * it is {@link PhotoOutlook}'s; each moment is one of the errand's offers, weighted by how much the
 * camera's settings say the pet fancies that kind of photo, and the decision draws among them.
 *
 * <p>It goes to a few blocks from who the photo is of, if it is of somebody, puts the camera up, presses the
 * shutter and holds the pose a little while after. The picture itself is drawn by the owner's game and comes
 * back a moment later; what becomes of it then is told to the pet's mind (see
 * {@link com.dwinovo.chiikawa.camera.PhotoResults}), so the pet is free of it as soon as it has held the pose.
 *
 * <p>Its priority is above picking things up and playing a scene, below a slip: a photo is rare
 * enough, with the camera's cooldown of half a day, that when there is a good moment it is
 * the thing to do, and nothing a pet works at is worth missing it for.
 *
 * <p>How long it rests is the camera's: a pack's camera says how long after a photo that was kept and how
 * long after one that was not, and the wait for the picture is a minute. These are the named numbers this
 * errand works out for the rests in its settings.
 */
public final class TakePhotoErrand implements ErrandType<TakePhotoErrand.Shot, ErrandTunings.TakePhoto> {
    /**
     * What it was admitted to take a photo of.
     *
     * @param photo the moment the pet's outlook offered
     */
    public record Shot(PhotoPlan photo) implements Plan {
        /** Who it goes to for the photo, as they are now: nowhere for a view, which it takes from where it stands. */
        @Override
        public Optional<GlobalPos> destination(PetView view) {
            return photo.subject().flatMap(view::whereIs);
        }

        /** The camera it is taken with, which its rests depend on. */
        @Override
        public Optional<ResourceLocation> restKey() {
            return Optional.of(photo.camera());
        }
    }

    @Override
    public ResourceLocation id() {
        return ErrandIds.TAKE_PHOTO;
    }

    @Override
    public ErrandCategory category() {
        return ErrandCategory.PHOTO;
    }

    @Override
    public Codec<ErrandTunings.TakePhoto> tuningCodec() {
        return ErrandTunings.TakePhoto.CODEC;
    }

    @Override
    public Offers<Shot> offers(PetView view, ErrandTunings.TakePhoto tuning) {
        PhotoOutlook outlook = view.photo();
        Check start = outlook.canStart();
        if (!start.ok()) {
            return Offers.none(start);
        }
        CameraSettings.Subjects fancies = outlook.camera().orElseThrow().subjects();
        List<Offer<Shot>> offers = new ArrayList<>();
        outlook.fancied().forEach(moment -> offers.add(new Offer<>(new Shot(moment), fancies.weight(moment.kind()))));
        return Offers.of(offers);
    }

    @Override
    public Check stillWanted(Shot plan, PetView view, ErrandTunings.TakePhoto tuning) {
        return view.photo().canContinue();
    }

    @Override
    public Routine routine(Shot plan, ErrandSettings<ErrandTunings.TakePhoto> settings) {
        ErrandTunings.TakePhoto tuning = settings.tuning();
        PhotoPlan photo = plan.photo();
        CameraSettings camera = CameraSettings.of(photo.camera()).orElseThrow();
        Optional<Goal> subject = photo.subject().map(Goal.Creature::new);
        Goal gaze = subject.orElseGet(() -> new Goal.Horizon(photo.facing(), tuning.viewDistance()));
        List<Routine.Part> parts = new ArrayList<>();
        // A few blocks from them, as a photographer stands.
        subject.ifPresent(goal -> parts.add(Routine.step(() -> new WalkTo(goal, tuning.speed(), tuning.standBack(),
            tuning.standBack() + 1.0, tuning.maxWalkTicks(), tuning.noWayTicks()))));
        parts.add(Routine.step(() -> new Aim(gaze, camera.aimTicks(), PetActivity.PHOTO)));
        parts.add(Routine.step(() -> new Shutter(photo, camera, tuning.lookBackTicks())));
        parts.add(Routine.step(() -> new Hold(tuning.holdTicks(), PetActivity.PHOTO, Optional.of(gaze),
            TakePhotoErrand::pictureBack)));
        return Routine.of(parts.toArray(Routine.Part[]::new));
    }

    /**
     * The camera is put away once the picture is back, or is not coming: a photo that was kept is over, and
     * one that was not is a try that failed and rests only for the retry.
     */
    private static StepStatus pictureBack(Message message) {
        return switch (message) {
            case Message.PhotoResult.Stored stored -> StepStatus.DONE;
            case Message.PhotoResult.Failed failed -> StepStatus.gaveUp("not_coming");
            default -> StepStatus.RUNNING;
        };
    }

    @Override
    public RestStore.Kind restStore() {
        return RestStore.photoCooldown();
    }

    @Override
    public Map<ResourceLocation, RestSources.Resolver> restSources() {
        return Map.of(
            RestSources.CAMERA_COOLDOWN, camera(CameraSettings::cooldownTicks),
            RestSources.CAMERA_RETRY, camera(CameraSettings::retryTicks),
            RestSources.PHOTO_UPLOAD_WAIT, context -> PhotoUploads.WAIT_TICKS);
    }

    /** A named number the camera of the rest says, nothing while no data pack makes a camera of that item. */
    private static RestSources.Resolver camera(ToIntFunction<CameraSettings> number) {
        return context -> context.key().flatMap(CameraSettings::of).map(number::applyAsInt).orElse(0);
    }
}
