package com.dwinovo.chiikawa.menu;

import java.util.function.Consumer;
import net.minecraft.world.inventory.DataSlot;

/**
 * An entity's id, carried to the client by two of a menu's data slots.
 *
 * <p>The game sends a data slot's value as a short. One slot would hold an id up to 32767 and
 * wrap round beyond it, which a world that has been going a while gets past, and a screen whose
 * client could not find its pet in the level would have the panel and none of what is on it.
 * Each slot takes sixteen bits, which is what travels.
 */
final class EntityIdSlots {
    private static final int HALF = 0xFFFF;

    private final DataSlot low = DataSlot.standalone();
    private final DataSlot high = DataSlot.standalone();

    /** Hands the slots to the menu, which has to add them in the same order on both sides. */
    void addTo(Consumer<DataSlot> menu) {
        menu.accept(low);
        menu.accept(high);
    }

    void set(int id) {
        low.set(id & HALF);
        high.set(id >>> 16);
    }

    int get() {
        return (high.get() & HALF) << 16 | (low.get() & HALF);
    }
}
