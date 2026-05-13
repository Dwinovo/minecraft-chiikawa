package com.dwinovo.chiikawa.item;

import com.dwinovo.chiikawa.music.MusicBoxSelection;
import com.dwinovo.chiikawa.music.ServerMusicSystem;
import com.dwinovo.chiikawa.network.MusicPayloads.MusicCatalogPayload;
import com.dwinovo.chiikawa.platform.Services;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class MusicBoxItem extends Item {
    public MusicBoxItem() {
        this(new Properties());
    }

    public MusicBoxItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            Services.NETWORK.sendToClient(serverPlayer, new MusicCatalogPayload(
                hand == InteractionHand.OFF_HAND ? 1 : 0,
                ServerMusicSystem.library(serverPlayer.level().getServer()).catalog(),
                true
            ));
        }
        return level.isClientSide()
            ? InteractionResultHolder.success(stack)
            : InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(
        ItemStack stack,
        TooltipContext context,
        List<Component> tooltipComponents,
        TooltipFlag tooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        MusicBoxSelection selection = MusicBoxSelection.get(stack);
        if (selection == null) {
            tooltipComponents.add(Component.translatable("tooltip.chiikawa.music_box.empty").withStyle(ChatFormatting.GRAY));
        } else {
            tooltipComponents.add(Component.translatable("tooltip.chiikawa.music_box.selected", selection.title()).withStyle(ChatFormatting.AQUA));
        }
    }
}
