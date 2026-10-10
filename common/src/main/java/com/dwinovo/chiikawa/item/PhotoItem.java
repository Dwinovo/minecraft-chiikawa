package com.dwinovo.chiikawa.item;

import com.dwinovo.chiikawa.init.InitDataComponents;
import com.dwinovo.chiikawa.network.PhotoPayloads;
import com.dwinovo.chiikawa.photo.PhotoData;
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

/**
 * A photo a pet took. The item remembers who is in it, when and by whom, and where the
 * picture is kept; use it to look at the picture.
 *
 * <p>The picture is the reader's to fetch, so the server only says "open it" and what to
 * show under it, as it does for the handbook: the item never has to know the screen.
 */
public class PhotoItem extends Item {
    public PhotoItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** @return what the photo is of and who took it, or nothing for a photo with nothing in it */
    public static PhotoData dataOf(ItemStack stack) {
        return stack.get(InitDataComponents.PHOTO.get());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        PhotoData data = dataOf(stack);
        if (data == null) {
            return InteractionResultHolder.pass(stack);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            Services.NETWORK.sendToClient(serverPlayer, new PhotoPayloads.OpenPhotoPayload(data));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        PhotoData data = dataOf(stack);
        if (data != null) {
            tooltip.add(data.caption().copy().withStyle(ChatFormatting.GRAY));
        }
    }
}
