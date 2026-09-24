package com.dwinovo.chiikawa.item;

import com.dwinovo.chiikawa.entity.PetRecall;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * A bell that calls your pets home, from another dimension if that is where they got to.
 * It costs nothing to ring and everything to ring again for half a minute: finding a pet
 * you have lost is a rescue, not a toll, but a bell that can be rung every tick is a way
 * of dragging pets about rather than of finding them.
 */
public class PetBellItem extends Item {
    /** Half a minute between rings. */
    private static final int COOLDOWN_TICKS = 600;

    public PetBellItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide() || !(player instanceof ServerPlayer owner)) {
            return InteractionResult.SUCCESS;
        }
        // What the ring turned up is said by the recall itself: pets in chunks nobody has
        // loaded take a few seconds to be read back, and the answer waits for them.
        PetRecall.ring(owner);
        level.playSound(null, player.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.0F, 1.4F);
        player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        return InteractionResult.CONSUME;
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
        tooltipAdder.accept(Component.translatable("tooltip.chiikawa.pet_bell").withStyle(ChatFormatting.GRAY));
    }
}
