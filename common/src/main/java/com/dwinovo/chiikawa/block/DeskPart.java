package com.dwinovo.chiikawa.block;

import java.util.Locale;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;

/**
 * The two halves of an exam desk, as a bed has a head and a foot: the desk, which faces the
 * way the desk faces, and the chair behind it that its pet sits on.
 */
public enum DeskPart implements StringRepresentable {
    DESK(""),
    CHAIR("_chair");

    /** What this half's model is called, after the desk's. */
    private final String modelSuffix;

    DeskPart(String modelSuffix) {
        this.modelSuffix = modelSuffix;
    }

    /** @return the model this half is drawn from, for a desk whose own model is {@code desk} */
    public Identifier model(Identifier desk) {
        return desk.withSuffix(modelSuffix);
    }

    /** @return the way from this half to the other, for a desk facing {@code facing} */
    public Direction toOther(Direction facing) {
        return this == DESK ? facing.getOpposite() : facing;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
