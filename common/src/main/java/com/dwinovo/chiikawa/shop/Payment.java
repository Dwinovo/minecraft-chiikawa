package com.dwinovo.chiikawa.shop;

import java.util.function.Predicate;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * Paying in things: counting what a holder has of them, and taking a price out in full or
 * not at all. What counts as payment is the caller's to say: money for a shop or a board's
 * level, as {@link Wallet} has it, or whatever an exam's fee is paid in.
 */
public final class Payment {
    private Payment() {
    }

    /** How much of what {@code pays} the holder has on it. */
    public static int count(Container container, Predicate<ItemStack> pays) {
        int held = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (pays.test(stack)) {
                held += stack.getCount();
            }
        }
        return held;
    }

    /**
     * Takes {@code price} out, from whichever slots it is in, or nothing at all when there
     * is not that much.
     *
     * @return whether it was paid. Nobody leaves half the money on the counter and walks
     *         off with half a cake
     */
    public static boolean take(Container container, int price, Predicate<ItemStack> pays) {
        if (price <= 0 || count(container, pays) < price) {
            return false;
        }
        int left = price;
        for (int slot = 0; slot < container.getContainerSize() && left > 0; slot++) {
            ItemStack stack = container.getItem(slot);
            if (!pays.test(stack)) {
                continue;
            }
            int taken = Math.min(left, stack.getCount());
            stack.shrink(taken);
            left -= taken;
            if (stack.isEmpty()) {
                container.setItem(slot, ItemStack.EMPTY);
            }
        }
        container.setChanged();
        return true;
    }
}
