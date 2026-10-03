package com.dwinovo.chiikawa.block;

import java.util.Optional;

/**
 * The sheet an exam desk has on it: the answer sheet while its pet is signed up and has not
 * handed in, the results the morning after, stamped passed or failed, and otherwise
 * nothing. Each sheet is a bone of the desk's model.
 */
public enum DeskSheet {
    NONE(Optional.empty()),
    ANSWER(Optional.of("AnswerSheet")),
    PASSED(Optional.of("PassedSheet")),
    FAILED(Optional.of("FailedSheet"));

    private final Optional<String> bone;

    DeskSheet(Optional<String> bone) {
        this.bone = bone;
    }

    /** @return the bone of the sheet on the desk, if any */
    public Optional<String> bone() {
        return bone;
    }

    /** @return whether {@code bone} is one of the sheets, which a desk shows only while it has it on it */
    public static boolean isSheet(String bone) {
        for (DeskSheet sheet : values()) {
            if (sheet.bone.filter(bone::equals).isPresent()) {
                return true;
            }
        }
        return false;
    }

    public static DeskSheet byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : NONE;
    }
}
