package com.dwinovo.chiikawa.task;

import com.dwinovo.chiikawa.Constants;
import net.minecraft.resources.Identifier;

/**
 * The kinds of work a pet reports while doing it, through {@link TaskTracker#advance}.
 * A slip counts one of them towards its target.
 */
public final class PetWorkCounters {
    /** One weed pulled up. */
    public static final Identifier WEED = id("weed");
    /** One mushroom picked. */
    public static final Identifier PICK_MUSHROOM = id("pick_mushroom");
    /** One hostile mob put down, by whichever hand or arrow did it. */
    public static final Identifier SLAY = id("slay");

    private PetWorkCounters() {
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Constants.MOD_ID, path);
    }
}
