package com.dwinovo.chiikawa.client.render;

import com.dwinovo.chiikawa.block.DeskPart;
import com.dwinovo.chiikawa.block.DeskSheet;
import com.dwinovo.chiikawa.block.ExamDeskBlockEntity;
import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws each half of an exam desk from a model of its own, as each half of a bed is drawn:
 * the desk, with only the sheet it has on it (see {@link DeskSheet}), and the chair.
 */
public final class ExamDeskRenderer extends PropBlockRenderer<ExamDeskBlockEntity> {
    /** What the chair's model is called, after the desk's. */
    private static final String CHAIR_SUFFIX = "_chair";

    @Override
    protected ResourceLocation model(ExamDeskBlockEntity half) {
        ResourceLocation desk = BuiltInRegistries.BLOCK.getKey(half.getBlockState().getBlock());
        return half.part() == DeskPart.DESK ? desk : desk.withSuffix(CHAIR_SUFFIX);
    }

    @Override
    protected Predicate<String> shown(ExamDeskBlockEntity half) {
        DeskSheet sheet = half.sheet();
        return bone -> !DeskSheet.isSheet(bone) || sheet.bone().filter(bone::equals).isPresent();
    }
}
