package com.dwinovo.chiikawa.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * An item with nothing to it but a line under its name saying what it is for:
 * {@code tooltip.<namespace>.<path>}, in grey, as the mod's other items say theirs.
 */
public class DescribedItem extends Item {
    public DescribedItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(this);
        tooltip.accept(Component.translatable("tooltip." + id.getNamespace() + "." + id.getPath())
            .withStyle(ChatFormatting.GRAY));
    }
}
