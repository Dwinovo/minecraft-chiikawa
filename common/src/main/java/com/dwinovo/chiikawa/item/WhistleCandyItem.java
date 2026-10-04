package com.dwinovo.chiikawa.item;

import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.init.InitDataComponents;
import com.dwinovo.chiikawa.whistle.PetWhistle;
import com.dwinovo.chiikawa.whistle.WhistleSettings;
import java.util.Optional;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * A candy that is also a whistle. Hold it to blow it: every pet of yours within earshot
 * takes the order the candy is set to, and the candy is eaten for it. Sneak and use it to
 * set it to the next order.
 *
 * <p>The order is the stack's own, so a handful of candy set to "stay" is a handful that
 * sits a yard down. How it blows is data, see {@link WhistleSettings}; who hears it and what
 * they do is {@link PetWhistle}, which knows nothing of this item.
 *
 * <p>Not food, so it never gets in the way of feeding a pet.
 */
public class WhistleCandyItem extends DescribedItem {
    /**
     * How long a game that does not have the item's settings holds the pose for. Only the
     * server blows, and ends the pose when it does; the settings are server data, so a
     * remote player's game cannot know how long a blow is and only needs to not cut it short.
     */
    private static final int POSE_TICKS = 72000;

    public WhistleCandyItem(Properties properties) {
        super(properties);
    }

    /** The order the candy blows, follow until it is switched. */
    public static PetDirective order(ItemStack stack) {
        return stack.getOrDefault(InitDataComponents.WHISTLE_MODE.get(), PetDirective.FOLLOW);
    }

    private static void setOrder(ItemStack stack, PetDirective order) {
        // Follow is what a candy is without a note, so a candy switched back stacks with the rest.
        if (order == PetDirective.FOLLOW) {
            stack.remove(InitDataComponents.WHISTLE_MODE.get());
        } else {
            stack.set(InitDataComponents.WHISTLE_MODE.get(), order);
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.chiikawa.whistle_candy.with_order", order(stack).orderName());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            PetDirective next = order(stack).next();
            setOrder(stack, next);
            if (!level.isClientSide()) {
                player.displayClientMessage(
                    Component.translatable("message.chiikawa.whistle_candy.switched", next.orderName()), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.TOOT_HORN;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return settings().map(WhistleSettings::blowTicks).orElse(POSE_TICKS);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        Optional<WhistleSettings> settings = settings();
        if (!(level instanceof ServerLevel server) || !(entity instanceof ServerPlayer owner) || settings.isEmpty()) {
            return stack;
        }
        server.playSound(null, owner.getX(), owner.getY(), owner.getZ(),
            SoundEvent.createVariableRangeEvent(settings.get().sound()), SoundSource.PLAYERS, 1.0F,
            settings.get().pitch());
        server.sendParticles(ParticleTypes.NOTE, owner.getX(), owner.getEyeY() + 0.3, owner.getZ(),
            3, 0.3, 0.1, 0.3, 1.0);
        PetDirective order = order(stack);
        PetWhistle.Heard heard = PetWhistle.blow(owner, order, settings.get());
        owner.displayClientMessage(result(order, heard), true);
        // Eaten whether or not anyone heard it: a blow is a blow.
        if (!owner.getAbilities().instabuild) {
            stack.shrink(1);
        }
        owner.getCooldowns().addCooldown(this, settings.get().cooldownTicks());
        return stack;
    }

    /** What the blow turned up, for the action bar. */
    private static Component result(PetDirective order, PetWhistle.Heard heard) {
        if (heard.owned() > 0) {
            return Component.translatable("message.chiikawa.whistle_candy." + order.name().toLowerCase(java.util.Locale.ROOT),
                heard.owned());
        }
        if (heard.wild() > 0) {
            return Component.translatable("message.chiikawa.whistle_candy.wild", heard.wild());
        }
        return Component.translatable("message.chiikawa.whistle_candy.nobody");
    }

    private Optional<WhistleSettings> settings() {
        return WhistleSettings.of(BuiltInRegistries.ITEM.getKey(this));
    }
}
