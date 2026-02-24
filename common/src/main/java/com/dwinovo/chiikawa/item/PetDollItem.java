package com.dwinovo.chiikawa.item;

import com.dwinovo.chiikawa.entity.AbstractPet;
import java.util.function.Supplier;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class PetDollItem extends Item {
    private final Supplier<? extends EntityType<? extends AbstractPet>> entityType;

    public PetDollItem(Item.Properties properties, Supplier<? extends EntityType<? extends AbstractPet>> entityType) {
        super(properties);
        this.entityType = entityType;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return tryStartCakeRitual(
            context.getLevel(),
            context.getPlayer(),
            context.getItemInHand(),
            context.getClickedPos()
        );
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public void appendHoverText(
        ItemStack stack,
        Item.TooltipContext context,
        TooltipDisplay tooltipDisplay,
        Consumer<Component> tooltipAdder,
        TooltipFlag tooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, tooltipFlag);
        tooltipAdder.accept(Component.translatable("tooltip.chiikawa.doll.place_on_cake").withStyle(ChatFormatting.GRAY));
    }

    public InteractionResult tryStartCakeRitual(
        Level level,
        @Nullable Player player,
        ItemStack stack,
        BlockPos clickedPos
    ) {
        BlockState clickedState = level.getBlockState(clickedPos);
        if (!CakeReviveRitual.isOfferingBlock(clickedState)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ServerLevel serverLevel = (ServerLevel) level;
        boolean started = PetReviveRitualManager.tryStartRitual(
            serverLevel,
            clickedPos,
            clickedState,
            player,
            stack,
            this.entityType
        );
        if (!started) {
            return InteractionResult.FAIL;
        }

        if (player != null) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        return InteractionResult.CONSUME;
    }
}
