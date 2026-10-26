package com.dwinovo.chiikawa.entity.brain.errand.work;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;

/**
 * What a work is done to, as its file names it: {@code "target": {"type": "foe"}}. A work whose action does
 * not take one has no {@code target}, and the file of an action that cannot use the one it names is turned off,
 * see {@link WorkAction#refuses}.
 */
public sealed interface WorkTarget {
    Codec<WorkTarget> CODEC = Kind.CODEC.dispatch("type", WorkTarget::kind, Kind::codec);

    /** @return which kind of target this is */
    Kind kind();

    /** The foe the pet's attack sensor names. */
    record Foe() implements WorkTarget {
        @Override
        public Kind kind() {
            return Kind.FOE;
        }
    }

    /** The kinds of target a file can name, by their {@code type}. */
    enum Kind implements StringRepresentable {
        FOE(MapCodec.unit(new Foe()));

        static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        private final MapCodec<? extends WorkTarget> codec;

        Kind(MapCodec<? extends WorkTarget> codec) {
            this.codec = codec;
        }

        MapCodec<? extends WorkTarget> codec() {
            return codec;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
