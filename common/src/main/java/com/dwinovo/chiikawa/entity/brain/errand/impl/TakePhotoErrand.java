package com.dwinovo.chiikawa.entity.brain.errand.impl;

import com.dwinovo.chiikawa.camera.PhotoPlan;
import com.dwinovo.chiikawa.entity.brain.errand.Check;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandTunings;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.step.LegacyActivityStep;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.init.InitActivity;
import com.dwinovo.chiikawa.init.InitMemory;
import java.util.Optional;
import net.minecraft.core.GlobalPos;

/**
 * Hachiware, with a camera and film in its backpack, takes a photo when there is something
 * worth one: its owner in sight, a friend caught in a little scene, a view at sunrise or
 * sunset or in a biome it has not photographed yet. What it is that makes a moment worth
 * it is {@link com.dwinovo.chiikawa.camera.PhotoOutlook}'s; this only asks.
 *
 * <p>Its priority is above picking things up and playing a scene, below a slip: a photo is rare
 * enough, with the camera's cooldown of half a day, that when there is a good moment it is
 * the thing to do, and nothing a pet works at is worth missing it for.
 */
public final class TakePhotoErrand extends LegacyErrand<TakePhotoErrand.Shot, ErrandTunings.TakePhoto> {
    /**
     * What it was admitted to take a photo of.
     *
     * @param photo the plan the pet's outlook offered
     */
    public record Shot(PhotoPlan photo) implements Plan {
        /** Who it goes to for the photo: nowhere for a view, which it takes from where it stands. */
        @Override
        public Optional<GlobalPos> destination(PetView view) {
            return view.photo().plan().flatMap(PhotoPlan::where);
        }
    }

    public TakePhotoErrand() {
        super(ErrandIds.TAKE_PHOTO, ErrandCategory.PHOTO, ErrandTunings.TakePhoto.CODEC, () -> InitActivity.TAKE_PHOTO.get());
    }

    @Override
    protected Check canStart(PetView view, ErrandTunings.TakePhoto tuning) {
        return view.photo().canStart();
    }

    @Override
    protected Shot plan(PetView view, ErrandTunings.TakePhoto tuning) {
        return new Shot(view.photo().plan().orElseThrow());
    }

    @Override
    public Check stillWanted(Shot plan, PetView view, ErrandTunings.TakePhoto tuning) {
        return view.photo().canContinue();
    }

    /** What it was admitted to do is what it does: the plan goes into its memory for the behavior. */
    @Override
    protected void onStart(Shot plan, LegacyActivityStep.Runtime runtime) {
        runtime.brain().setMemory(InitMemory.PHOTO_PLAN.get(), plan.photo());
    }

    @Override
    protected void onStop(LegacyActivityStep.Runtime runtime) {
        runtime.brain().eraseMemory(InitMemory.PHOTO_PLAN.get());
    }
}
