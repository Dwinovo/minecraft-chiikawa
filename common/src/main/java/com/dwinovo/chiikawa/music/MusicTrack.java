package com.dwinovo.chiikawa.music;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record MusicTrack(
    String trackId,
    String title,
    int durationTicks,
    int frameCount,
    MusicTrackStatus status,
    String sourceHash,
    String sourceFile,
    String cacheFile,
    String error
) {
    public static final Codec<MusicTrack> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("track_id").forGetter(MusicTrack::trackId),
        Codec.STRING.fieldOf("title").forGetter(MusicTrack::title),
        Codec.INT.fieldOf("duration_ticks").forGetter(MusicTrack::durationTicks),
        Codec.INT.fieldOf("frame_count").forGetter(MusicTrack::frameCount),
        MusicTrackStatus.CODEC.fieldOf("status").forGetter(MusicTrack::status),
        Codec.STRING.fieldOf("source_hash").forGetter(MusicTrack::sourceHash),
        Codec.STRING.fieldOf("source_file").forGetter(MusicTrack::sourceFile),
        Codec.STRING.fieldOf("cache_file").forGetter(MusicTrack::cacheFile),
        Codec.STRING.optionalFieldOf("error", "").forGetter(MusicTrack::error)
    ).apply(instance, MusicTrack::new));

    public static final Codec<java.util.List<MusicTrack>> LIST_CODEC = CODEC.listOf();

    public MusicTrack ready(int durationTicks, int frameCount, String cacheFile) {
        return new MusicTrack(trackId, title, durationTicks, frameCount, MusicTrackStatus.READY,
            sourceHash, sourceFile, cacheFile, "");
    }

    public MusicTrack importing() {
        return new MusicTrack(trackId, title, 0, 0, MusicTrackStatus.IMPORTING,
            sourceHash, sourceFile, cacheFile, "");
    }

    public MusicTrack failed(String message) {
        return new MusicTrack(trackId, title, 0, 0, MusicTrackStatus.FAILED,
            sourceHash, sourceFile, cacheFile, message == null ? "" : message);
    }
}
