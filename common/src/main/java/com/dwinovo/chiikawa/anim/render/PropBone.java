package com.dwinovo.chiikawa.anim.render;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Things a pet has with it only while it does something: bones of its model, under the same
 * name for every pet that has one, there only while one of their animations plays. The
 * pencil in its hand while it writes an exam, put down as it hands the paper in; the sheet it
 * pushes across the desk as it hands it in, which takes the place of the desk's own;
 * Hachiware's guitar while it plays; and its camera while it takes a photo, held up to its
 * face. A pet whose model lacks one goes without.
 *
 * <p>Each of these is in the pet's hands while it is out, so whatever it holds is put away
 * meanwhile: a sword does not stay in the fist that writes, nor sit across the guitar. The
 * camera is only in its hands while it is held up; hanging at its chest, it is not a prop in this
 * sense, and the sword stays in the fist.
 */
public enum PropBone {
    EXAM_PAPER("ExamPaper", ShownDuring.any("hand_in")),
    EXAM_PENCIL("ExamPencil", ShownDuring.any("exam").unless("hand_in")),
    GUITAR("guitar", ShownDuring.any("guitar")),
    CAMERA("camera", ShownDuring.any("photo"));

    private final String bone;
    private final ShownDuring shownDuring;

    PropBone(String bone, ShownDuring shownDuring) {
        this.bone = bone;
        this.shownDuring = shownDuring;
    }

    public String bone() {
        return bone;
    }

    public ShownDuring shownDuring() {
        return shownDuring;
    }

    /** What a pet does with its hands, with no prop of its own to show for it: it holds up the sheet of results to read. */
    private static final String READ_RESULTS = "read_results";

    /**
     * @return the animations in which a pet's hands or chest are for something other than the
     *         camera, which therefore hangs out of the way while they play
     */
    public static List<String> animationsClashingWithCamera() {
        return Stream.concat(
                Arrays.stream(values()).filter(prop -> prop != CAMERA)
                    .flatMap(prop -> prop.shownDuring.animations().stream()),
                Stream.of(READ_RESULTS))
            .distinct()
            .toList();
    }

    /** @return the prop {@code bone} is, if it is one */
    public static Optional<PropBone> of(String bone) {
        for (PropBone prop : values()) {
            if (prop.bone.equals(bone)) {
                return Optional.of(prop);
            }
        }
        return Optional.empty();
    }
}
