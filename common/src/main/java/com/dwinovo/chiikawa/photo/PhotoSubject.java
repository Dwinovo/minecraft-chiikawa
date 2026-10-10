package com.dwinovo.chiikawa.photo;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/** What a photo is of: the three kinds of subject a photographer picks between. */
public enum PhotoSubject implements StringRepresentable {
    /** The owner. */
    OWNER,
    /** A friend: another pet. */
    FRIEND,
    /** A view, with nobody in particular in it. */
    SCENERY;

    public static final Codec<PhotoSubject> CODEC = StringRepresentable.fromEnum(PhotoSubject::values);
    public static final StreamCodec<ByteBuf, PhotoSubject> STREAM_CODEC =
        ByteBufCodecs.idMapper(index -> values()[index], PhotoSubject::ordinal);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
