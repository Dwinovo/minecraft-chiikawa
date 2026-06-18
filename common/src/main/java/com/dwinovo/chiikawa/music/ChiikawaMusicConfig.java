package com.dwinovo.chiikawa.music;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ChiikawaMusicConfig(
    boolean enabled,
    int streamRadius,
    int maxConcurrentSessions,
    int maxTrackSeconds,
    int opusBitrate,
    int framesPerChunk,
    int jitterBufferChunks,
    int leadSeconds
) {
    public static final ChiikawaMusicConfig DEFAULT = new ChiikawaMusicConfig(
        true,
        32,
        4,
        600,
        48000,
        5,
        3,
        2
    );

    public static final Codec<ChiikawaMusicConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.optionalFieldOf("enabled", DEFAULT.enabled).forGetter(ChiikawaMusicConfig::enabled),
        Codec.INT.optionalFieldOf("stream_radius", DEFAULT.streamRadius).forGetter(ChiikawaMusicConfig::streamRadius),
        Codec.INT.optionalFieldOf("max_concurrent_sessions", DEFAULT.maxConcurrentSessions).forGetter(ChiikawaMusicConfig::maxConcurrentSessions),
        Codec.INT.optionalFieldOf("max_track_seconds", DEFAULT.maxTrackSeconds).forGetter(ChiikawaMusicConfig::maxTrackSeconds),
        Codec.INT.optionalFieldOf("opus_bitrate", DEFAULT.opusBitrate).forGetter(ChiikawaMusicConfig::opusBitrate),
        Codec.INT.optionalFieldOf("frames_per_chunk", DEFAULT.framesPerChunk).forGetter(ChiikawaMusicConfig::framesPerChunk),
        Codec.INT.optionalFieldOf("jitter_buffer_chunks", DEFAULT.jitterBufferChunks).forGetter(ChiikawaMusicConfig::jitterBufferChunks),
        Codec.INT.optionalFieldOf("lead_seconds", DEFAULT.leadSeconds).forGetter(ChiikawaMusicConfig::leadSeconds)
    ).apply(instance, ChiikawaMusicConfig::new));

    public int frameSamples() {
        return 960;
    }

    /** Lookahead window in Opus frames (each frame is 20ms, so 50 frames per second). */
    public int leadFrames() {
        return Math.max(framesPerChunk, leadSeconds * 50);
    }

    public int sampleRate() {
        return 48000;
    }

    public int channels() {
        return 1;
    }
}
