package com.dwinovo.chiikawa.anim.render;

import java.util.Optional;

/**
 * Things a pet has with it only while it does something: bones of its model, under the same
 * name for every pet that has one, there only while one of their animations plays. The
 * pencil in its hand while it writes an exam, and the sheet it holds out as it hands it in -
 * the sheet it writes on is the desk's; Hachiware's guitar while it plays. A pet whose model
 * lacks one goes without.
 */
public enum PropBone {
    EXAM_PAPER("ExamPaper", ShownDuring.any("hand_in")),
    EXAM_PENCIL("ExamPencil", ShownDuring.any("exam")),
    GUITAR("guitar", ShownDuring.any("guitar"));

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
