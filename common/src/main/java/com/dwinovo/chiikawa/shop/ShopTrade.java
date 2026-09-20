package com.dwinovo.chiikawa.shop;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * One trade over a counter, from the customer's side: money out and goods in, or the
 * other way about.
 *
 * <p>Either the whole trade happens or none of it does. A customer who paid and got
 * nothing, or handed goods over for no money, was robbed by a bug — so each of these
 * takes the step that can fail first, and only then the step that cannot.
 */
public final class ShopTrade {
    private ShopTrade() {
    }

    /**
     * Buys one.
     *
     * @return whether it went through; false when the shop does not sell it, the customer
     *         cannot afford it, or has nowhere to put it
     */
    public static boolean buy(Inventory customer, ShopCatalog.Entry entry) {
        if (entry.buy() <= 0 || Wallet.count(customer) < entry.buy()) {
            return false;
        }
        // Handed over before it is paid for: if there is no room, nothing has been spent.
        if (!customer.add(new ItemStack(entry.item()))) {
            return false;
        }
        Wallet.pay(customer, entry.buy());
        return true;
    }

    /**
     * Sells one.
     *
     * @return whether it went through; false when the shop does not want it, the customer
     *         has none, or there is no room for the money
     */
    public static boolean sell(Inventory customer, ShopCatalog.Entry entry) {
        if (entry.sell() <= 0 || !take(customer, entry)) {
            return false;
        }
        if (!customer.add(new ItemStack(Items.EMERALD, entry.sell()))) {
            // No room for the money, so the goods go back where they came from.
            customer.add(new ItemStack(entry.item()));
            return false;
        }
        return true;
    }

    /** Takes one of the entry's item out of the customer's hands. */
    private static boolean take(Inventory customer, ShopCatalog.Entry entry) {
        for (int slot = 0; slot < customer.getContainerSize(); slot++) {
            ItemStack stack = customer.getItem(slot);
            if (!stack.is(entry.item())) {
                continue;
            }
            stack.shrink(1);
            if (stack.isEmpty()) {
                customer.setItem(slot, ItemStack.EMPTY);
            }
            customer.setChanged();
            return true;
        }
        return false;
    }
}
