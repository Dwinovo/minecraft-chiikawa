package com.dwinovo.chiikawa.testing;

import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;

/**
 * Gives every item its components, as loading a world does. An item's components are bound
 * with the registries a world loads rather than when the item is registered, so a test that
 * makes an {@code ItemStack} after a bare bootstrap binds them first.
 */
public final class ItemComponents {
    private ItemComponents() {
    }

    public static void bind() {
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(VanillaRegistries.createLookup())
            .forEach(DataComponentInitializers.PendingComponents::apply);
    }
}
