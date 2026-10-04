package com.dwinovo.chiikawa.client.render;

import com.dwinovo.chiikawa.block.DeskSheet;
import com.dwinovo.chiikawa.block.ExamDeskBlock;
import com.dwinovo.chiikawa.block.ExamDeskBlockEntity;
import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/**
 * Draws each half of an exam desk from a model of its own, as each half of a bed is drawn:
 * the desk, with only the sheet it has on it (see {@link DeskSheet}), and the chair.
 */
public final class ExamDeskRenderer extends PropBlockRenderer<ExamDeskBlockEntity> {
    @Override
    protected Identifier model(ExamDeskBlockEntity half) {
        return half.part().model(BuiltInRegistries.BLOCK.getKey(half.getBlockState().getBlock()));
    }

    @Override
    protected Predicate<String> shown(ExamDeskBlockEntity half) {
        ExamDeskBlock desk = (ExamDeskBlock) half.getBlockState().getBlock();
        DeskSheet sheet = half.sheet();
        return bone -> desk.shownAtRest(bone) || sheet.bone().filter(bone::equals).isPresent();
    }
}
