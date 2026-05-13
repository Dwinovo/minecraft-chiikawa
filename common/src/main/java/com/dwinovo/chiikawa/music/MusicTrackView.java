package com.dwinovo.chiikawa.music;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record MusicTrackView(
    String trackId,
    String title,
    int durationTicks,
    MusicTrackStatus status,
    String error
) {
    public static final StreamCodec<FriendlyByteBuf, MusicTrackView> STREAM_CODEC = StreamCodec.of(
        (buffer, value) -> {
            buffer.writeUtf(value.trackId);
            buffer.writeUtf(value.title);
            buffer.writeVarInt(value.durationTicks);
            buffer.writeEnum(value.status);
            buffer.writeUtf(value.error);
        },
        buffer -> new MusicTrackView(
            buffer.readUtf(),
            buffer.readUtf(),
            buffer.readVarInt(),
            buffer.readEnum(MusicTrackStatus.class),
            buffer.readUtf()
        )
    );

    public static MusicTrackView of(MusicTrack track) {
        return new MusicTrackView(track.trackId(), track.title(), track.durationTicks(), track.status(), track.error());
    }
}
