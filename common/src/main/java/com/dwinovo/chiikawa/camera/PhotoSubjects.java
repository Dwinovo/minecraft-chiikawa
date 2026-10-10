package com.dwinovo.chiikawa.camera;

import com.dwinovo.chiikawa.photo.PhotoSubject;
import java.util.Optional;
import java.util.Set;
import net.minecraft.util.RandomSource;

/**
 * What a pet takes a photo of, out of what there is to take one of right now: the kinds it
 * has a good moment for, picked in proportion to how much it fancies each. Pure, so it can
 * be asked without a pet or a world.
 */
public final class PhotoSubjects {
    private PhotoSubjects() {
    }

    /**
     * @param available the kinds of photo there is a good moment for now
     * @param weights how much the pet fancies each kind; a kind it does not fancy at all is never picked
     * @param random the pet's own random source
     * @return the kind to take, or nothing when there is no good moment for any kind it fancies
     */
    public static Optional<PhotoSubject> choose(Set<PhotoSubject> available, CameraSettings.Subjects weights,
            RandomSource random) {
        int total = 0;
        for (PhotoSubject subject : PhotoSubject.values()) {
            if (available.contains(subject)) {
                total += weights.weight(subject);
            }
        }
        if (total <= 0) {
            return Optional.empty();
        }
        int roll = random.nextInt(total);
        for (PhotoSubject subject : PhotoSubject.values()) {
            if (available.contains(subject)) {
                roll -= weights.weight(subject);
                if (roll < 0) {
                    return Optional.of(subject);
                }
            }
        }
        return Optional.empty();
    }
}
