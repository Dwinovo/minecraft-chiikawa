package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.camera.CameraSettings;
import com.dwinovo.chiikawa.camera.PhotoDelivery;
import com.dwinovo.chiikawa.camera.PhotoPlan;
import com.dwinovo.chiikawa.camera.PhotoScout;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.Cue;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import com.dwinovo.chiikawa.photo.PhotoSystem;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Presses the shutter: the owner's game is asked to draw the picture, and what comes of it is told to the
 * pet's mind when it is back (see {@code PhotoDelivery}). A photo is posed or candid, rolled each time (see
 * {@link CameraSettings#posed}). Posed, the photographer calls out, and whoever is being photographed, if it is
 * a pet, is cued to turn to the camera and strike a pose of its own. Candid, nobody says a word and a pet being
 * photographed is left entirely alone: it goes on with what it was doing, which is what the photo is of.
 *
 * <p>An action of one tick, so it is never cut half way.
 */
public final class Shutter implements Step {
    /** The pose whoever is photographed strikes. */
    private static final String POSE = "photographed";

    private final PhotoPlan photo;
    private final CameraSettings camera;
    private final int lookBackTicks;

    /**
     * @param photo what is photographed
     * @param camera how
     * @param lookBackTicks how long whoever is photographed keeps looking at the camera
     */
    public Shutter(PhotoPlan photo, CameraSettings camera, int lookBackTicks) {
        this.photo = photo;
        this.camera = camera;
        this.lookBackTicks = lookBackTicks;
    }

    @Override
    public void begin(Handle handle) {
    }

    @Override
    public StepStatus tick(Handle handle) {
        LiveHandle live = LiveHandle.of(handle);
        AbstractPet pet = live.pet();
        if (camera.posed(pet.getRandom())) {
            handle.say(VoiceMoment.PHOTO);
            photo.subject().flatMap(live::creature).filter(AbstractPet.class::isInstance).map(AbstractPet.class::cast)
                .ifPresent(friend -> handle.cue(friend,
                    new Cue.Pose(pet.getUUID(), lookBackTicks, POSE, VoiceMoment.PHOTOGRAPHED)));
        }
        if (pet.getOwner() instanceof ServerPlayer owner && pet.level() instanceof ServerLevel level) {
            PhotoSystem.of(level.getServer()).request(owner, pet, camera,
                new PhotoDelivery(pet, photo.camera(), photo.kind(), photo.name(), PhotoScout.biomeOf(pet)));
        }
        return StepStatus.DONE;
    }

    @Override
    public PetActivity pose() {
        return PetActivity.PHOTO;
    }
}
