package com.dwinovo.chiikawa.item;

import net.minecraft.world.item.ItemStack;

/**
 * An item that says for itself whether it is ready to be given away, which a pet asks before
 * anything else when it looks through its bag for a present. A roll of film is ready only once
 * it is full, though film is something a pet keeps for itself; an unfinished one is not.
 *
 * <p>An item without this is judged by the usual rules: not money, not something its pet
 * keeps.
 */
public interface GiftReadiness {
    /** Whether this stack of the item is ready to be somebody's present. Decides it, one way or the other. */
    boolean readyToGive(ItemStack stack);
}
