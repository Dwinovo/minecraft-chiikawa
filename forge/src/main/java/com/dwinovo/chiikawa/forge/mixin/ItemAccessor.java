package com.dwinovo.chiikawa.forge.mixin;

import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Forge's slot for an item's client extensions. On this version only the item's own class
 * fills it, from {@code Item#initializeClient}, and the mod's items are built in common code
 * that cannot override that; this is the way in for {@code ChiikawaForgeClient}.
 */
@Mixin(Item.class)
public interface ItemAccessor {
    @Accessor(value = "renderProperties", remap = false)
    void chiikawa$setRenderProperties(Object properties);
}
