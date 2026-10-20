package com.dwinovo.chiikawa.camera;

import com.dwinovo.chiikawa.entity.brain.errand.Check;
import java.util.List;
import java.util.Optional;

/**
 * Whether a pet could take a photo now, as plain facts, the way the rest of what an errand
 * reads is: who it is, what it carries, how its owner is, and what there is to take a photo
 * of. Made by {@link PhotoScout}, read by the errand, and tried without a world.
 *
 * <p>Whether the pet is resting from photos is not here: that is the rest ledger's to say.
 *
 * @param photographer whether this kind of pet takes photos at all
 * @param camera the settings of the camera it carries, if it carries one
 * @param hasFilm whether it has film for that camera
 * @param ownerInRange whether its owner is in its world and near enough for their game to draw what it sees
 * @param ownerCalm whether its owner is in no state for it: not hurt just now, riding, or flying, where the
 *                  game that has to draw the picture is working hard enough already
 * @param moments the photos there is a good moment for now, one of each kind, whatever the pet fancies
 */
public record PhotoOutlook(boolean photographer, Optional<CameraSettings> camera, boolean hasFilm,
                           boolean ownerInRange, boolean ownerCalm, List<PhotoPlan> moments) {
    /** What a pet that takes no photos has. */
    public static final PhotoOutlook NONE = new PhotoOutlook(false, Optional.empty(), false, false, false, List.of());

    public PhotoOutlook {
        moments = List.copyOf(moments);
    }

    /** Whether it may set about taking a photo. */
    public Check canStart() {
        Check able = canContinue();
        if (!able.ok()) {
            return able;
        }
        return fancied().isEmpty() ? Check.fail("nothing_to_shoot") : Check.OK;
    }

    /**
     * Whether it may carry on with one. The moment may pass while it aims, which is no reason
     * to put the camera away mid-shot.
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
        return ownerCalm ? Check.OK : Check.fail("owner_busy");
    }

    /** @return the moments of a kind the pet fancies taking at all, which are the ones it may choose among */
    public List<PhotoPlan> fancied() {
        return camera.map(settings -> moments.stream().filter(moment -> settings.subjects().weight(moment.kind()) > 0).toList())
            .orElse(List.of());
    }
}
