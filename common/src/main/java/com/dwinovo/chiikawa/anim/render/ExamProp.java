package com.dwinovo.chiikawa.anim.render;

import java.util.Optional;

/**
 * What a pet has with it at a licence exam: the answer sheet in front of it and the pencil in
 * its hand, each a bone of its model under the same name for every pet, there only while it
 * sits the exam, and the sheet while it hands it in. A pet whose model lacks one goes
 * without.
 */
public enum ExamProp {
    PAPER("ExamPaper", ShownDuring.any("exam", "hand_in")),
    PENCIL("ExamPencil", ShownDuring.any("exam"));

    private final String bone;
    private final ShownDuring shownDuring;

    ExamProp(String bone, ShownDuring shownDuring) {
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
    public static Optional<ExamProp> of(String bone) {
        for (ExamProp prop : values()) {
            if (prop.bone.equals(bone)) {
                return Optional.of(prop);
            }
        }
        return Optional.empty();
    }
}
