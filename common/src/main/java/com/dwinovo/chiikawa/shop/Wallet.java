package com.dwinovo.chiikawa.shop;

import com.dwinovo.chiikawa.init.InitTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * Money, counted, spent and paid out. What money is, is the {@code chiikawa:currency} item
 * tag — emeralds unless a pack says otherwise, and then anything it names — and this is the
 * one place that asks. Every item in the tag is worth one; what a shop pays out in, and what
 * a screen pictures and a message names, is the first.
 */
public final class Wallet {
    private Wallet() {
    }

    /** Whether a stack is money. */
    public static boolean isMoney(ItemStack stack) {
        return stack.is(InitTag.CURRENCY);
    }

    /**
     * @param amount how much
     * @return that much of the first thing in the currency tag, or nothing when a pack has
     *         left the tag empty and there is no money in this world at all
     */
    public static ItemStack coins(int amount) {
        return BuiltInRegistries.ITEM.get(InitTag.CURRENCY)
            .flatMap(tag -> tag.stream().findFirst())
            .map(item -> new ItemStack(item, amount))
            .orElse(ItemStack.EMPTY);
    }

    /** How much the holder has on it. */
    public static int count(Container container) {
        return Payment.count(container, Wallet::isMoney);
    }

    /**
     * Takes {@code price} out, or nothing at all when there is not that much.
     *
     * @return whether it was paid. A shop is not somewhere a pet leaves half the money and
     *         walks off with half a cake
     */
    public static boolean pay(Container container, int price) {
        return Payment.take(container, price, Wallet::isMoney);
    }
}
