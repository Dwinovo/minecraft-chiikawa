package com.dwinovo.chiikawa.task;

import com.dwinovo.chiikawa.Constants;
import net.minecraft.resources.Identifier;

/**
 * The kinds of work a pet reports while doing it, through {@link TaskTracker#advance}.
 * A slip counts one of them towards its target. The works' own are the {@code counter} of their errand files, see
 * {@link com.dwinovo.chiikawa.data.WorkData}; this one is reported by the death hook, and what names it is the hunts, which
 * are the works a slip for putting monsters down is for.
 */
public final class PetWorkCounters {
    /** One hostile mob put down, by whichever hand or arrow did it. */
    public static final Identifier SLAY = id("slay");

    private PetWorkCounters() {
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Constants.MOD_ID, path);
    }
}
