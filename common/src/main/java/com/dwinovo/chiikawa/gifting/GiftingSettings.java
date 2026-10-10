package com.dwinovo.chiikawa.gifting;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * How pets give their owners presents, loaded from {@code data/<namespace>/pet_gifting/gifting.json}
 * by {@link GiftingSettingsLoader}: one file for the whole mod, so a pack can replace it outright.
 *
 * <p>Server-side only. With no file loaded no pet gives presents: a mod without its settings does
 * nothing rather than guessing.
 *
 * @param cooldownTicks the least time a pet waits after giving a present before it thinks of another
 * @param weights how much a pet fancies giving each kind of thing; the first line that fits a thing
 *                says, and a thing no line fits weighs 1
 */
public record GiftingSettings(int cooldownTicks, List<Weighted> weights) {
    /** What a thing weighs that no line is about. */
    public static final int UNLISTED_WEIGHT = 1;

    /**
     * One line of the list: an item, or {@code #} and a tag for every item in it, and how much it weighs.
     */
    public record Weighted(Either<TagKey<Item>, Item> match, int weight) {
        /** Lazy because the item registry only exists once the game has bootstrapped. */
        public static final Codec<Weighted> CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.create(instance -> instance.group(
            Codec.either(TagKey.hashedCodec(Registries.ITEM), BuiltInRegistries.ITEM.byNameCodec())
                .fieldOf("item").forGetter(Weighted::match),
            ExtraCodecs.POSITIVE_INT.fieldOf("weight").forGetter(Weighted::weight)
        ).apply(instance, Weighted::new)));

        public boolean matches(ItemStack stack) {
            return match.map(stack::is, stack::is);
        }
    }

    public static final Codec<GiftingSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ExtraCodecs.NON_NEGATIVE_INT.fieldOf("cooldown_ticks").forGetter(GiftingSettings::cooldownTicks),
        Weighted.CODEC.listOf().fieldOf("weights").forGetter(GiftingSettings::weights)
    ).apply(instance, GiftingSettings::new));

    private static volatile Optional<GiftingSettings> current = Optional.empty();

    public GiftingSettings {
        weights = List.copyOf(weights);
    }

    /** @return how a pet gives presents, or nothing when no data pack says */
    public static Optional<GiftingSettings> current() {
        return current;
    }

    static void replace(Optional<GiftingSettings> settings) {
        current = settings;
    }

    /** @return how much a pet fancies giving this */
    public int weightOf(ItemStack stack) {
        return weights.stream().filter(line -> line.matches(stack)).map(Weighted::weight).findFirst()
            .orElse(UNLISTED_WEIGHT);
    }
}
