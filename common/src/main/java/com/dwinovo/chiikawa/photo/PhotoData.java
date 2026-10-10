package com.dwinovo.chiikawa.photo;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;

/**
 * What a roll of film remembers about one shot: which photo, who is in it, when it was taken
 * and by whom. The picture itself is not in here but in the world's {@link PhotoLibrary},
 * where the id leads to it; the roll stays small however big the photos are.
 *
 * @param photo the id the picture is kept under
 * @param subject what the photo is of
 * @param name who is in it: the owner or the friend by name, and nothing for a view
 * @param day the day of the world it was taken on, counting the first as 1
 * @param photographer who took it, by name
 */
public record PhotoData(UUID photo, PhotoSubject subject, String name, long day, String photographer) {
    public static final Codec<PhotoData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        UUIDUtil.CODEC.fieldOf("photo").forGetter(PhotoData::photo),
        PhotoSubject.CODEC.fieldOf("subject").forGetter(PhotoData::subject),
        Codec.STRING.fieldOf("name").forGetter(PhotoData::name),
        Codec.LONG.fieldOf("day").forGetter(PhotoData::day),
        Codec.STRING.fieldOf("photographer").forGetter(PhotoData::photographer)
    ).apply(instance, PhotoData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PhotoData> STREAM_CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC, PhotoData::photo,
        PhotoSubject.STREAM_CODEC, PhotoData::subject,
        ByteBufCodecs.STRING_UTF8, PhotoData::name,
        ByteBufCodecs.VAR_LONG, PhotoData::day,
        ByteBufCodecs.STRING_UTF8, PhotoData::photographer,
        PhotoData::new);

    /** A photo taken in this level now. */
    public static PhotoData taken(UUID photo, PhotoSubject subject, String name, Level level, String photographer) {
        return new PhotoData(photo, subject, name, level.getDayTime() / Level.TICKS_PER_DAY + 1, photographer);
    }

    /** "Taken by Hachiware · Day 12 · Chiikawa", the line under a photo in the album. */
    public Component caption() {
        return subject == PhotoSubject.SCENERY
            ? Component.translatable("screen.chiikawa.album.caption.scenery", photographer, day)
            : Component.translatable("screen.chiikawa.album.caption", photographer, day, name);
    }
}
