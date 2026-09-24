package com.dwinovo.chiikawa.client.ui.mc;

import com.dwinovo.chiikawa.ui.Icon;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * An item, handed to the ui library as an {@link Icon} it never looks inside and gives
 * back to the surface that drew the screen.
 */
public record ItemIcon(ItemStack stack) implements Icon {
    /**
     * @param item what to picture something as
     * @return that item, or {@link Icon#NONE} when nothing is registered under the id —
     *         a data pack naming an item that is not there leaves an empty box, not a crash
     */
    public static Icon of(Identifier item) {
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(item));
        return stack.isEmpty() ? Icon.NONE : new ItemIcon(stack);
    }
}
