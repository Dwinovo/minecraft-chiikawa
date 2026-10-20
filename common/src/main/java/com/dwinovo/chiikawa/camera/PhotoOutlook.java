package com.dwinovo.chiikawa.camera;

import com.dwinovo.chiikawa.entity.brain.errand.Check;
import java.util.Optional;

/**
 * Whether a pet could take a photo now, as plain facts, the way the rest of what an errand
 * reads is: who it is, what it carries, how its owner is, and what there is to take a photo
 * of. Made by {@link PhotoScout}, read by the errand, and tried without a world.
 *
 * @param photographer whether this kind of pet takes photos at all
 * @param camera the settings of the camera it carries, if it carries one
 * @param hasFilm whether it has film for that camera
 * @param ownerInRange whether its owner is in its world and near enough for their game to draw what it sees
 * @param ownerCalm whether its owner is in no state for it: not hurt just now, riding, or flying, where the
 *                  game that has to draw the picture is working hard enough already
 * @param coolingDown whether it took a photo, or tried to, a moment ago
 * @param plan what it has chosen to take a photo of: a plan it already has, or one drawn from the
 *             moments there are by what it fancies
 */
public record PhotoOutlook(boolean photographer, Optional<CameraSettings> camera, boolean hasFilm,
                           boolean ownerInRange, boolean ownerCalm, boolean coolingDown, Optional<PhotoPlan> plan) {
    /** What a pet that takes no photos has. */
    public static final PhotoOutlook NONE = new PhotoOutlook(false, Optional.empty(), false, false, false, false,
        Optional.empty());

    /** Whether it may set about taking a photo. */
    public Check canStart() {
        Check able = canContinue();
        if (!able.ok()) {
            return able;
        }
        return plan.isEmpty() ? Check.fail("nothing_to_shoot") : Check.OK;
    }

    /**
     * Whether it may carry on with one. The moment may pass while it aims, which is no reason
     * to put the camera away mid-shot; the rest it takes once the shutter has gone is what
     * ends it, so a pet is not left holding up a camera with nothing to do.
     */
    public Check canContinue() {
        if (!photographer) {
            return Check.fail("not_photographer");
        }
        if (camera.isEmpty()) {
            return Check.fail("no_camera");
        }
        if (!hasFilm) {
            return Check.fail("no_film");
        }
        if (!ownerInRange) {
            return Check.fail("owner_far");
        }
        if (!ownerCalm) {
            return Check.fail("owner_busy");
        }
        return coolingDown ? Check.fail("photo_resting") : Check.OK;
    }
}
