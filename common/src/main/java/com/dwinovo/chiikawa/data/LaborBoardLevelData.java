package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.task.BoardLevels;
import java.util.List;

/**
 * The generated labor board levels (gameplay doc, section 10): a board puts up three slips
 * a day when it is placed, and each level an owner buys adds one, dearer each time.
 */
public final class LaborBoardLevelData {
    public static final BoardLevels LEVELS = new BoardLevels(List.of(
        new BoardLevels.Level(3, 0),
        new BoardLevels.Level(4, 16),
        new BoardLevels.Level(5, 32)));

    private LaborBoardLevelData() {
    }
}
