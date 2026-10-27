package com.dwinovo.chiikawa.entity.brain.errand.pastime;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

/**
 * Where a pet goes to pass the time, as a pastime's file names it: {@code {"type": "owner"}}. Only what the pet is to look for
 * is said here; where that is, now, is for the pet to see, see {@code VisitAction}.
 */
public sealed interface Destination {
    Codec<Destination> CODEC = Kind.CODEC.dispatch("type", Destination::kind, Kind::codec);

    /** @return which kind of place this is */
    Kind kind();

    /** The owner, wherever they are. */
    record ToOwner() implements Destination {
        static final MapCodec<ToOwner> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.point(new ToOwner()));

        @Override
        public Kind kind() {
            return Kind.OWNER;
        }
    }

    /** The nearest place out of the rain. */
    record ToShelter() implements Destination {
        static final MapCodec<ToShelter> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.point(new ToShelter()));

        @Override
        public Kind kind() {
            return Kind.SHELTER;
        }
    }

    /**
     * The nearest creature of a kind that the pet sees.
     *
     * @param kinds the kinds, by the tag that names them
     */
    record ToCreature(TagKey<EntityType<?>> kinds) implements Destination {
        static final MapCodec<ToCreature> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            TagKey.hashedCodec(Registries.ENTITY_TYPE).fieldOf("entities").forGetter(ToCreature::kinds)
        ).apply(instance, ToCreature::new));

        @Override
        public Kind kind() {
            return Kind.CREATURE;
        }
    }

    /**
     * The nearest thing of a kind that the pet sees lying about.
     *
     * @param kinds the kinds, by the tag that names them
     */
    record ToItem(TagKey<Item> kinds) implements Destination {
        static final MapCodec<ToItem> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            TagKey.hashedCodec(Registries.ITEM).fieldOf("items").forGetter(ToItem::kinds)
        ).apply(instance, ToItem::new));

        @Override
        public Kind kind() {
            return Kind.ITEM;
        }
    }

    /** The kinds of destination a file can name, by their {@code type}. */
    enum Kind implements StringRepresentable {
        OWNER(ToOwner.CODEC),
        SHELTER(ToShelter.CODEC),
        CREATURE(ToCreature.CODEC),
        ITEM(ToItem.CODEC);

        static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        private final MapCodec<? extends Destination> codec;

        Kind(MapCodec<? extends Destination> codec) {
            this.codec = codec;
        }

        MapCodec<? extends Destination> codec() {
            return codec;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
