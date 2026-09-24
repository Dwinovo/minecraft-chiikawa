package com.dwinovo.chiikawa.utils;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.RecordBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviderType;
import net.minecraft.world.item.Item;

/**
 * The mod's data read and written the way later versions of the game read and write it, so
 * a data pack is the same files on every version the mod is built for. 1.20.4's own codecs
 * differ in two places the mod's data goes through:
 *
 * <ul>
 *   <li>An item named by an id nobody registered decodes as air rather than failing.
 *   <li>A value provider writes its fields under {@code "value"} instead of beside its
 *       {@code "type"}.
 * </ul>
 */
public final class ModCodecs {
    /**
     * An item by id, failing on an id nobody registered rather than turning it into air.
     * Lazy, as the codecs here are, because the registries only exist once the game has
     * bootstrapped, and reading a board's levels must not need them.
     */
    public static final Codec<Item> ITEM = ExtraCodecs.lazyInitializedCodec(() -> ResourceLocation.CODEC.comapFlatMap(
        id -> BuiltInRegistries.ITEM.getOptional(id)
            .map(DataResult::success)
            .orElseGet(() -> DataResult.error(() -> "Unknown registry key in minecraft:item: " + id)),
        item -> BuiltInRegistries.ITEM.getKey(item)));

    /** A whole number: a plain number for a constant, otherwise its fields beside its type. */
    public static final Codec<IntProvider> INT_PROVIDER = Codec.either(Codec.INT, new InlineIntProvider()).xmap(
        either -> either.map(ConstantInt::of, provider -> provider),
        provider -> provider.getType() == IntProviderType.CONSTANT
            ? Either.left(((ConstantInt) provider).getValue())
            : Either.right(provider));

    /** As {@link #INT_PROVIDER}, never below one. */
    public static final Codec<IntProvider> POSITIVE_INT_PROVIDER =
        ExtraCodecs.lazyInitializedCodec(() -> IntProvider.codec(1, Integer.MAX_VALUE, INT_PROVIDER));

    private ModCodecs() {
    }

    /** A value provider's own fields in the same object as its {@code "type"}. */
    private static final class InlineIntProvider implements Codec<IntProvider> {
        private static final String TYPE = "type";

        @Override
        public <T> DataResult<Pair<IntProvider, T>> decode(DynamicOps<T> ops, T input) {
            return ops.getMap(input).flatMap(map -> {
                T type = map.get(TYPE);
                if (type == null) {
                    return DataResult.error(() -> "Missing " + TYPE + " in " + input);
                }
                return BuiltInRegistries.INT_PROVIDER_TYPE.byNameCodec().parse(ops, type)
                    .flatMap(providerType -> providerType.codec().parse(ops, input))
                    .map(provider -> Pair.of((IntProvider) provider, ops.empty()));
            });
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> DataResult<T> encode(IntProvider input, DynamicOps<T> ops, T prefix) {
            Codec<IntProvider> codec = (Codec<IntProvider>) input.getType().codec();
            return codec.encodeStart(ops, input).flatMap(ops::getMap).flatMap(fields -> {
                RecordBuilder<T> builder = ops.mapBuilder();
                builder.add(TYPE, BuiltInRegistries.INT_PROVIDER_TYPE.byNameCodec().encodeStart(ops, input.getType()));
                fields.entries().forEach(field -> builder.add(field.getFirst(), field.getSecond()));
                return builder.build(prefix);
            });
        }
    }
}
