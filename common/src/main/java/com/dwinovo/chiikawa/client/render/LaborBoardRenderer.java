package com.dwinovo.chiikawa.client.render;

import com.dwinovo.chiikawa.block.LaborBoardBlockEntity;
import java.util.function.Predicate;

/**
 * Draws a labor board with a plate on each hook whose slip is still waiting: the plates a
 * pet takes down come down on the board too, so how much work is left shows from across the
 * garden. Each plate is a bone of the model, {@code Slip0} on.
 */
public final class LaborBoardRenderer extends PropBlockRenderer<LaborBoardBlockEntity> {
    /** What the plate bones are called, before the place they hang at. */
    public static final String PLATE_BONE = "Slip";

    @Override
    protected Predicate<String> shown(LaborBoardBlockEntity board) {
        int hanging = board.hanging();
        return bone -> {
            if (!bone.startsWith(PLATE_BONE)) {
                return true;
            }
            int place = Integer.parseInt(bone.substring(PLATE_BONE.length()));
            return (hanging & 1 << place) != 0;
        };
    }
}
