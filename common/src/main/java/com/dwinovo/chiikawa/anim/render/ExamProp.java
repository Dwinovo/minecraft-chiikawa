package com.dwinovo.chiikawa.anim.render;

import java.util.Optional;

/**
 * What a pet has with it at a licence exam: the pencil in its hand while it writes, and the
 * sheet it holds out as it hands it in - the sheet it writes on is the desk's. Each is a
 * bone of its model under the same name for every pet; a pet whose model lacks one goes
 * without.
 */
public enum ExamProp {
    PAPER("ExamPaper", ShownDuring.any("hand_in")),
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
