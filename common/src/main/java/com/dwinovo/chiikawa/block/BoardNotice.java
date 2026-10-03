package com.dwinovo.chiikawa.block;

import com.dwinovo.chiikawa.qualification.QualificationExam;
import java.util.Optional;

/**
 * The sheet a labor board has pinned up about licence exams, so a player sees from across
 * the garden that an exam is on or that results are out: the exam notice while one opened
 * there is on, then the results the morning after, until the next notice goes up. Each
 * sheet is a bone of the board's model.
 */
public enum BoardNotice {
    NONE(Optional.empty()),
    EXAM(Optional.of("ExamNotice")),
    RESULTS(Optional.of("ExamResults"));

    private final Optional<String> bone;

    BoardNotice(Optional<String> bone) {
        this.bone = bone;
    }

    /** @return the bone of the sheet pinned up, if any */
    public Optional<String> bone() {
        return bone;
    }

    /** @return whether {@code bone} is one of the sheets, which a board shows only while it is pinned up */
    public static boolean isSheet(String bone) {
        for (BoardNotice notice : values()) {
            if (notice.bone.filter(bone::equals).isPresent()) {
                return true;
            }
        }
        return false;
    }

    /**
     * What a board has pinned up now: the exam notice while an exam opened there is on, or
     * while anyone is still at the paper; otherwise the results, while it has any posted.
     */
    public static BoardNotice of(long dayTime, long gameTime, BoardExam exam) {
        if (exam.isOn(dayTime) || exam.seats().anyTaken(gameTime)) {
            return EXAM;
        }
        return exam.results().posted(QualificationExam.day(dayTime)).isEmpty() ? NONE : RESULTS;
    }

    public static BoardNotice byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : NONE;
    }
}
