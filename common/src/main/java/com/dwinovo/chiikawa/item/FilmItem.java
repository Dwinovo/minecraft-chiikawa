package com.dwinovo.chiikawa.item;

import com.dwinovo.chiikawa.init.InitDataComponents;
import com.dwinovo.chiikawa.network.PhotoPayloads;
import com.dwinovo.chiikawa.photo.FilmRoll;
import com.dwinovo.chiikawa.platform.Services;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * A roll of film, which is also the album of what was taken on it. Fresh film stacks and does
 * nothing; once a roll has a shot on it, it is a roll of its own, and using it opens the pages
 * of what it holds.
 *
 * <p>The pictures are the reader's to fetch, so the server only says "open it" and what is on
 * the roll, as it does for the handbook: the item never has to know the screen.
 */
public class FilmItem extends DescribedItem implements GiftReadiness {
    public FilmItem(Properties properties) {
        super(properties);
    }

    /** @return what is on this roll, nothing for fresh film */
    public static FilmRoll rollOf(ItemStack stack) {
        return stack.getOrDefault(InitDataComponents.FILM_ROLL.get(), FilmRoll.NONE);
    }

    /** A pet's own film is not given away until it has been used up: a full roll is a present, the rest is film. */
    @Override
    public boolean readyToGive(ItemStack stack) {
        return rollOf(stack).full();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        FilmRoll roll = rollOf(stack);
        if (!roll.started()) {
            return InteractionResultHolder.pass(stack);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            Services.NETWORK.sendToClient(serverPlayer, new PhotoPayloads.OpenAlbumPayload(roll.shots()));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        FilmRoll roll = rollOf(stack);
        if (roll.started()) {
            tooltip.add(Component.translatable("tooltip.chiikawa.film.shots", roll.shots().size(), roll.exposures())
                .withStyle(ChatFormatting.GRAY));
        } else {
            super.appendHoverText(stack, context, tooltip, flag);
        }
    }
}
