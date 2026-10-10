package com.dwinovo.chiikawa.camera;

import com.dwinovo.chiikawa.entity.AbstractPet;
import net.minecraft.world.Container;

/**
 * Where a pet has a camera, as a player's game needs to know it: a camera hangs at the pet's
 * chest wherever it is carried, and is never also drawn in the fist. Worked out on the server, as
 * only the server knows which items are cameras, and synced as its ordinal, so constants must
 * never be reordered.
 */
public enum CameraCarry {
    /** No camera in the backpack. */
    NONE,
    /** A camera in some slot other than the main hand. */
    POCKET,
    /** A camera in the main-hand slot. */
    HAND;

    private static final CameraCarry[] VALUES = values();

    /** @return where the backpack has a camera, the hand first */
    public static CameraCarry of(Container backpack) {
        if (PhotoScout.cameraOf(backpack.getItem(AbstractPet.MAINHAND_SLOT)).isPresent()) {
            return HAND;
        }
        for (int slot = AbstractPet.MAINHAND_SLOT + 1; slot < backpack.getContainerSize(); slot++) {
            if (PhotoScout.cameraOf(backpack.getItem(slot)).isPresent()) {
                return POCKET;
            }
        }
        return NONE;
    }

    /** @return whether the camera hangs at the chest */
    public boolean onChest() {
        return this != NONE;
    }

    /** @return the state as synced; anything unknown is none */
    public static CameraCarry fromId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : NONE;
    }
}
