package com.dwinovo.chiikawa.shop;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Money, counted and spent. Emeralds are the only money in this world — a pet earns them
 * off a labor board and hands them over at a shop — so what counts as money is decided
 * here and nowhere else.
 */
public final class Wallet {
    private Wallet() {
    }

    /** How much the holder has on it. */
    public static int count(Container container) {
        int emeralds = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.is(Items.EMERALD)) {
                emeralds += stack.getCount();
            }
        }
        return emeralds;
    }

    /**
     * Takes {@code price} out, or nothing at all when there is not that much.
     *
     * @return whether it was paid. A shop is not somewhere a pet leaves half the money and
     *         walks off with half a cake
     */
    public static boolean pay(Container container, int price) {
        if (price <= 0 || count(container) < price) {
            return false;
        }
        int left = price;
        for (int slot = 0; slot < container.getContainerSize() && left > 0; slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.is(Items.EMERALD)) {
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
