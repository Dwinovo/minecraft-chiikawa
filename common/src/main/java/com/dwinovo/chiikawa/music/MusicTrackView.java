package com.dwinovo.chiikawa.music;

import net.minecraft.network.FriendlyByteBuf;

public record MusicTrackView(
    String trackId,
    String title,
    int durationTicks,
    MusicTrackStatus status,
    String error
) {
    public static MusicTrackView read(FriendlyByteBuf buffer) {
        return new MusicTrackView(
            buffer.readUtf(),
            buffer.readUtf(),
            buffer.readVarInt(),
            buffer.readEnum(MusicTrackStatus.class),
            buffer.readUtf()
        );
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(trackId);
        buffer.writeUtf(title);
        buffer.writeVarInt(durationTicks);
        buffer.writeEnum(status);
        buffer.writeUtf(error);
    }

    public static MusicTrackView of(MusicTrack track) {
        return new MusicTrackView(track.trackId(), track.title(), track.durationTicks(), track.status(), track.error());
    }
}
