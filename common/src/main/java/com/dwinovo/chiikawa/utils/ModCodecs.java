package com.dwinovo.chiikawa.utils;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviderType;
import net.minecraft.world.item.Item;

/**
 * The mod's data read and written the way later versions of the game read and write it, so
 * a data pack is the same files on every version the mod is built for. 1.20.1's own codecs
 * differ in three places the mod's data goes through:
 *
 * <ul>
 *   <li>{@code optionalFieldOf} quietly falls back to the default when a value is there
 *       but wrong, so a typo would load as if the line were not there. The mod's loaders
 *       report it instead; this is the {@code strictOptionalField} vanilla gains in 1.20.2.
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

    public static <A> MapCodec<Optional<A>> strictOptionalField(Codec<A> codec, String name) {
        return new StrictOptionalFieldCodec<>(name, codec);
    }

    public static <A> MapCodec<A> strictOptionalField(Codec<A> codec, String name, A defaultValue) {
        return strictOptionalField(codec, name).xmap(
            value -> value.orElse(defaultValue),
            value -> Objects.equals(value, defaultValue) ? Optional.empty() : Optional.of(value));
    }

    private static final class StrictOptionalFieldCodec<A> extends MapCodec<Optional<A>> {
        private final String name;
        private final Codec<A> elementCodec;

        private StrictOptionalFieldCodec(String name, Codec<A> elementCodec) {
            this.name = name;
            this.elementCodec = elementCodec;
        }

        @Override
        public <T> DataResult<Optional<A>> decode(DynamicOps<T> ops, MapLike<T> input) {
            T value = input.get(name);
            return value == null ? DataResult.success(Optional.empty()) : elementCodec.parse(ops, value).map(Optional::of);
        }

        @Override
        public <T> RecordBuilder<T> encode(Optional<A> input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            return input.isPresent() ? prefix.add(name, elementCodec.encodeStart(ops, input.get())) : prefix;
        }

        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.of(ops.createString(name));
        }
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
