package com.dwinovo.chiikawa.shop;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

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
     * Buys one lot: {@link ShopCatalog.Entry#count()} items for the price.
     *
     * @return whether it went through; false when the shop does not sell it, the customer
     *         cannot afford it, or has nowhere to put it
     */
    public static boolean buy(Inventory customer, ShopCatalog.Entry entry) {
        if (entry.buy() <= 0 || Wallet.count(customer) < entry.buy()) {
            return false;
        }
        // Handed over before it is paid for: if there is no room, nothing has been spent.
        if (!give(customer, entry)) {
            return false;
        }
        Wallet.pay(customer, entry.buy());
        return true;
    }

    /** Puts the whole lot in the customer's pockets, or none of it. */
    private static boolean give(Inventory customer, ShopCatalog.Entry entry) {
        ItemStack lot = new ItemStack(entry.item(), entry.count());
        if (!fits(customer, lot)) {
            return false;
        }
        customer.add(lot);
        return true;
    }

    /** Whether there is room for all of the lot, which {@link Inventory#add} alone cannot say. */
    private static boolean fits(Inventory customer, ItemStack lot) {
        int room = 0;
        for (ItemStack slot : customer.items) {
            if (slot.isEmpty()) {
                room += lot.getMaxStackSize();
            } else if (ItemStack.isSameItemSameTags(slot, lot)) {
                room += slot.getMaxStackSize() - slot.getCount();
            }
        }
        return room >= lot.getCount();
    }

    /**
     * Sells one lot: {@link ShopCatalog.Entry#count()} items for the price.
     *
     * @return whether it went through; false when the shop does not want it, the customer
     *         has fewer than a lot, there is no room for the money, or a pack has left the world without
     *         any money to pay in
     */
    public static boolean sell(Inventory customer, ShopCatalog.Entry entry) {
        ItemStack payment = Wallet.coins(entry.sell());
        if (entry.sell() <= 0 || payment.isEmpty() || !take(customer, entry)) {
            return false;
        }
        if (!customer.add(payment)) {
            // No room for the money, so the goods go back where they came from.
            customer.add(new ItemStack(entry.item(), entry.count()));
            return false;
        }
        return true;
    }

    /** Takes a lot of the entry's item out of the customer's hands, all of it or none. */
    private static boolean take(Inventory customer, ShopCatalog.Entry entry) {
        int held = 0;
        for (int slot = 0; slot < customer.getContainerSize(); slot++) {
            ItemStack stack = customer.getItem(slot);
            if (stack.is(entry.item())) {
                held += stack.getCount();
            }
        }
        if (held < entry.count()) {
            return false;
        }
        int owed = entry.count();
        for (int slot = 0; slot < customer.getContainerSize() && owed > 0; slot++) {
            ItemStack stack = customer.getItem(slot);
            if (!stack.is(entry.item())) {
                continue;
            }
            int taken = Math.min(owed, stack.getCount());
            stack.shrink(taken);
            owed -= taken;
            if (stack.isEmpty()) {
                customer.setItem(slot, ItemStack.EMPTY);
            }
        }
        customer.setChanged();
        return true;
    }
}
