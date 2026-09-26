package com.dwinovo.chiikawa.client.render;

import com.dwinovo.chiikawa.block.BoardNotice;
import com.dwinovo.chiikawa.block.LaborBoardBlockEntity;
import java.util.function.Predicate;

/**
 * Draws a labor board with a plate on each hook whose slip is still waiting: the plates a
 * pet takes down come down on the board too, so how much work is left shows from across the
 * garden. Each plate is a bone of the model, {@code Slip0} on. Of the sheets about exams,
 * only the one the board has pinned up shows; see {@link BoardNotice}.
 */
public final class LaborBoardRenderer extends PropBlockRenderer<LaborBoardBlockEntity> {
    /** What the plate bones are called, before the place they hang at. */
    public static final String PLATE_BONE = "Slip";

    @Override
    protected Predicate<String> shown(LaborBoardBlockEntity board) {
        int hanging = board.hanging();
        BoardNotice notice = board.notice();
        return bone -> {
            if (BoardNotice.isSheet(bone)) {
                return notice.bone().filter(bone::equals).isPresent();
            }
            if (!bone.startsWith(PLATE_BONE)) {
                return true;
            }
            int place = Integer.parseInt(bone.substring(PLATE_BONE.length()));
            return (hanging & 1 << place) != 0;
        };
    }
}
