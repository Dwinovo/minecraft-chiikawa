package com.dwinovo.chiikawa.music;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record MusicBoxSelection(String trackId, String title, int revision) {
    public static final Codec<MusicBoxSelection> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("track_id").forGetter(MusicBoxSelection::trackId),
        Codec.STRING.fieldOf("title").forGetter(MusicBoxSelection::title),
        Codec.INT.fieldOf("revision").forGetter(MusicBoxSelection::revision)
    ).apply(instance, MusicBoxSelection::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MusicBoxSelection> STREAM_CODEC = StreamCodec.of(
        (buffer, value) -> {
            buffer.writeUtf(value.trackId);
            buffer.writeUtf(value.title);
            buffer.writeVarInt(value.revision);
        },
        buffer -> new MusicBoxSelection(buffer.readUtf(), buffer.readUtf(), buffer.readVarInt())
    );

    public static MusicBoxSelection get(ItemStack stack) {
        return stack.get(com.dwinovo.chiikawa.init.InitDataComponents.MUSIC_BOX_SELECTION.get());
    }

    public String signature() {
        return trackId + ":" + revision;
    }
}
