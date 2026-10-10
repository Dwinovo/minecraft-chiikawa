package com.dwinovo.chiikawa.photo;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

/**
 * What a roll of film has on it: the shots taken so far, each remembering which photo it is,
 * who is in it, when it was taken and by whom. The pictures themselves are not in here but in
 * the world's {@link PhotoLibrary}, where the photo's id leads to them; the roll stays small.
 *
 * <p>A roll knows how many shots it holds, which is how many its camera had for it when it
 * was begun, so a roll is full or not without asking anything of the world. Film that has not
 * been shot has no roll at all.
 *
 * @param exposures how many shots the roll holds
 * @param shots what has been taken, in order
 */
public record FilmRoll(int exposures, List<PhotoData> shots) {
    /** The most shots a roll may be asked to hold, which a screen must not be sent more than. */
    public static final int MOST_EXPOSURES = 64;

    public static final FilmRoll NONE = new FilmRoll(0, List.of());

    public static final Codec<FilmRoll> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ExtraCodecs.intRange(1, MOST_EXPOSURES).fieldOf("exposures").forGetter(FilmRoll::exposures),
        PhotoData.CODEC.listOf().fieldOf("shots").forGetter(FilmRoll::shots)
    ).apply(instance, FilmRoll::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FilmRoll> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, FilmRoll::exposures,
        PhotoData.STREAM_CODEC.apply(ByteBufCodecs.list(MOST_EXPOSURES)), FilmRoll::shots,
        FilmRoll::new);

    public FilmRoll {
        shots = List.copyOf(shots);
    }

    /** A roll with its first shot. */
    public static FilmRoll begun(int exposures, PhotoData first) {
        return new FilmRoll(exposures, List.of(first));
    }

    /** Whether any shot has been taken on it. Film that has none is fresh, and not yet a roll to speak of. */
    public boolean started() {
        return !shots.isEmpty();
    }

    /** Whether it has no room for another shot. */
    public boolean full() {
        return started() && shots.size() >= exposures;
    }

    /** @return the same with one more shot on it */
    public FilmRoll with(PhotoData shot) {
        List<PhotoData> more = new ArrayList<>(shots);
        more.add(shot);
        return new FilmRoll(exposures, more);
    }
}
