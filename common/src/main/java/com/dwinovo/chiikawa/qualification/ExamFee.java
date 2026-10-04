package com.dwinovo.chiikawa.qualification;

import com.dwinovo.chiikawa.shop.Payment;
import com.dwinovo.chiikawa.utils.ModCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * What an owner pays at a board to open a licence's exam: once for the sitting, however
 * many of their pets go.
 *
 * @param item what it is paid in
 * @param count how many
 */
public record ExamFee(Item item, int count) {
    /** Lazy because the item registry only exists once the game has bootstrapped. */
    public static final Codec<ExamFee> CODEC = ExtraCodecs.lazyInitializedCodec(() -> RecordCodecBuilder.create(instance -> instance.group(
        ModCodecs.ITEM.fieldOf("item").forGetter(ExamFee::item),
        ExtraCodecs.POSITIVE_INT.fieldOf("count").forGetter(ExamFee::count)
    ).apply(instance, ExamFee::new)));

    /** How much of it the holder has on it. */
    public int heldIn(Container container) {
        return Payment.count(container, this::pays);
    }

    /** @return whether it was paid: in full, or not at all */
    public boolean payFrom(Container container) {
        return Payment.take(container, count, this::pays);
    }

    private boolean pays(ItemStack stack) {
        return stack.is(item);
    }
}
