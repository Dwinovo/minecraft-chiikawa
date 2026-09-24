package com.dwinovo.chiikawa.item;

import com.dwinovo.chiikawa.network.ManualPayloads;
import com.dwinovo.chiikawa.platform.Services;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * The handbook: a four-panel strip for each thing there is to know, acted out by the pets
 * themselves. A new player is handed one; another is a book and some pink dye.
 *
 * <p>The pages are the reader's own resources, so the server only says "open it": the item
 * never has to know the screen, which the server does not have.
 */
public class HandbookItem extends Item {
    public HandbookItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            Services.NETWORK.sendToClient(serverPlayer, ManualPayloads.OpenHandbookPayload.INSTANCE);
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(
        ItemStack stack,
        TooltipContext context,
        TooltipDisplay tooltipDisplay,
        Consumer<Component> tooltipAdder,
        TooltipFlag tooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, tooltipFlag);
        tooltipAdder.accept(Component.translatable("tooltip.chiikawa.handbook").withStyle(ChatFormatting.GRAY));
    }
}
