package com.dwinovo.chiikawa.gifting;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
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
 * @param cooldownTicks the least time between two presents of one pet, from when it gave the first
 * @param ownerDailyLimit the most presents an owner is given in a game day, all their pets together
 * @param fadeTicks how long a pet that is ready to give a present has to do so before it forgets it
 * @param occasions the chance each occasion puts a present in a pet's mind, and after how long; an occasion left out
 *                  never does
 * @param gifts what a pet may give from its bag besides what it set aside and what it likes
 * @param never what a pet never gives, whatever else is said of it
 */
public record GiftingSettings(int cooldownTicks, int ownerDailyLimit, int fadeTicks,
                              Map<GiftOccasion, OccasionSettings> occasions, List<Gift> gifts, TagKey<Item> never) {
    /**
     * How an occasion puts a present in a pet's mind.
     *
     * @param chance how likely it does, from 0 to 1
     * @param delayTicks how long after the occasion the pet is ready to give it: a pet does not answer being fed at once
     */
    public record OccasionSettings(float chance, int delayTicks) {
        public static final Codec<OccasionSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(OccasionSettings::chance),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("delay_ticks", 0).forGetter(OccasionSettings::delayTicks)
        ).apply(instance, OccasionSettings::new));
    }

    /**
     * One line of the list of gifts: an item, or a tag of them, and how much a pet fancies giving it; and whose gift it is.
     *
     * @param match the item, or every item of the tag
     * @param weight how much a pet fancies giving it against the other gifts it could
     * @param characters the kinds of pet that give it, by entity type; every kind if none is named
     */
    public record Gift(Either<Item, TagKey<Item>> match, int weight, List<ResourceLocation> characters) {
        /** Lazy because the item registry only exists once the game has bootstrapped. */
        public static final Codec<Gift> CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.create(instance -> instance.group(
            Codec.mapEither(BuiltInRegistries.ITEM.byNameCodec().fieldOf("item"),
                TagKey.hashedCodec(Registries.ITEM).fieldOf("items")).forGetter(Gift::match),
            ExtraCodecs.POSITIVE_INT.fieldOf("weight").forGetter(Gift::weight),
            ResourceLocation.CODEC.listOf().optionalFieldOf("characters", List.of()).forGetter(Gift::characters)
        ).apply(instance, Gift::new)));

        public Gift {
            characters = List.copyOf(characters);
        }

        /** @return whether this is a gift of that thing */
        public boolean matches(ItemStack stack) {
            return match.map(stack::is, stack::is);
        }

        /** @return whether a pet of that kind gives it */
        public boolean isFor(ResourceLocation character) {
            return characters.isEmpty() || characters.contains(character);
        }
    }

    public static final Codec<GiftingSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ExtraCodecs.NON_NEGATIVE_INT.fieldOf("cooldown_ticks").forGetter(GiftingSettings::cooldownTicks),
        ExtraCodecs.NON_NEGATIVE_INT.fieldOf("owner_daily_limit").forGetter(GiftingSettings::ownerDailyLimit),
        ExtraCodecs.POSITIVE_INT.fieldOf("fade_ticks").forGetter(GiftingSettings::fadeTicks),
        Codec.unboundedMap(GiftOccasion.CODEC, OccasionSettings.CODEC).fieldOf("occasions").forGetter(GiftingSettings::occasions),
        Gift.CODEC.listOf().fieldOf("gifts").forGetter(GiftingSettings::gifts),
        TagKey.hashedCodec(Registries.ITEM).fieldOf("never").forGetter(GiftingSettings::never)
    ).apply(instance, GiftingSettings::new));

    private static volatile Optional<GiftingSettings> current = Optional.empty();

    public GiftingSettings {
        occasions = Map.copyOf(occasions);
        gifts = List.copyOf(gifts);
    }

    /** @return how a pet gives presents, or nothing when no data pack says */
    public static Optional<GiftingSettings> current() {
        return current;
    }

    /** Replaces them, as a data pack reload does. Public so an in-game case can try other settings. */
    public static void replace(Optional<GiftingSettings> settings) {
        current = settings;
    }

    /** @return how this occasion puts a present in a pet's mind, or nothing if it never does */
    public Optional<OccasionSettings> occasion(GiftOccasion occasion) {
        return Optional.ofNullable(occasions.get(occasion));
    }

    /** @return the first line of the list of gifts that is a gift of this thing from a pet of this kind, if there is one */
    public Optional<Gift> giftOf(ItemStack stack, ResourceLocation character) {
        return gifts.stream().filter(gift -> gift.isFor(character) && gift.matches(stack)).findFirst();
    }
}
