package com.dwinovo.chiikawa.item;

import com.dwinovo.chiikawa.init.InitDataComponents;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/** The labor board as an item: says how far the board was paid up, when the item carries it. */
public class LaborBoardItem extends BlockItem {
    public LaborBoardItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
        Integer level = stack.get(InitDataComponents.BOARD_LEVEL.get());
        if (level != null) {
            tooltip.accept(Component.translatable("tooltip.chiikawa.labor_board.level", level).withStyle(ChatFormatting.GRAY));
        }
    }
}
