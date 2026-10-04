package com.dwinovo.chiikawa.whistle;

import net.minecraft.core.GlobalPos;

/**
 * A whistle a wild pet heard, held in a brain memory until it has lost interest. The
 * memory's expiry is the curiosity running out; {@code from} is the moment the pet sets
 * off for it.
 *
 * @param where the blower, as they stood when they blew
 * @param from the game time the pet starts for it
 */
public record WhistleHeard(GlobalPos where, long from) {
}
