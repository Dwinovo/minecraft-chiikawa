package com.dwinovo.chiikawa.network;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.music.MusicStopReason;
import com.dwinovo.chiikawa.music.MusicTrackView;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class MusicPayloads {
    private MusicPayloads() {
    }

    public record MusicCatalogPayload(int handIndex, List<MusicTrackView> tracks, boolean openScreen) implements CustomPacketPayload {
        public static final Type<MusicCatalogPayload> TYPE = payloadType("music_catalog");
        public static final StreamCodec<RegistryFriendlyByteBuf, MusicCatalogPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                buffer.writeVarInt(value.handIndex);
                buffer.writeCollection(value.tracks, (buf, track) -> MusicTrackView.STREAM_CODEC.encode(buf, track));
                buffer.writeBoolean(value.openScreen);
            },
            buffer -> new MusicCatalogPayload(
                buffer.readVarInt(),
                buffer.readList(buf -> MusicTrackView.STREAM_CODEC.decode(buf)),
                buffer.readBoolean()
            )
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record MusicBoxSelectTrackPayload(int handIndex, String trackId) implements CustomPacketPayload {
        public static final Type<MusicBoxSelectTrackPayload> TYPE = payloadType("music_box_select_track");
        public static final StreamCodec<RegistryFriendlyByteBuf, MusicBoxSelectTrackPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                buffer.writeVarInt(value.handIndex);
                buffer.writeUtf(value.trackId);
            },
            buffer -> new MusicBoxSelectTrackPayload(buffer.readVarInt(), buffer.readUtf())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record MusicCatalogRequestPayload(int handIndex, boolean rescan) implements CustomPacketPayload {
        public static final Type<MusicCatalogRequestPayload> TYPE = payloadType("music_catalog_request");
        public static final StreamCodec<RegistryFriendlyByteBuf, MusicCatalogRequestPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                buffer.writeVarInt(value.handIndex);
                buffer.writeBoolean(value.rescan);
            },
            buffer -> new MusicCatalogRequestPayload(buffer.readVarInt(), buffer.readBoolean())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record MusicStreamStartPayload(
        int sessionId,
        int entityId,
        String trackId,
        String title,
        int startFrame,
        int sampleRate,
        int channels,
        int frameSamples,
        int framesPerChunk,
        int jitterBufferChunks,
        int streamRadius
    ) implements CustomPacketPayload {
        public static final Type<MusicStreamStartPayload> TYPE = payloadType("music_stream_start");
        public static final StreamCodec<RegistryFriendlyByteBuf, MusicStreamStartPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                buffer.writeVarInt(value.sessionId);
                buffer.writeVarInt(value.entityId);
                buffer.writeUtf(value.trackId);
                buffer.writeUtf(value.title);
                buffer.writeVarInt(value.startFrame);
                buffer.writeVarInt(value.sampleRate);
                buffer.writeVarInt(value.channels);
                buffer.writeVarInt(value.frameSamples);
                buffer.writeVarInt(value.framesPerChunk);
                buffer.writeVarInt(value.jitterBufferChunks);
                buffer.writeVarInt(value.streamRadius);
            },
            buffer -> new MusicStreamStartPayload(
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readUtf(),
                buffer.readUtf(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt()
            )
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record MusicStreamChunkPayload(int sessionId, int frameStart, List<byte[]> frames) implements CustomPacketPayload {
        public static final Type<MusicStreamChunkPayload> TYPE = payloadType("music_stream_chunk");
        public static final StreamCodec<RegistryFriendlyByteBuf, MusicStreamChunkPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                buffer.writeVarInt(value.sessionId);
                buffer.writeVarInt(value.frameStart);
                buffer.writeCollection(value.frames, (buf, frame) -> buf.writeByteArray(frame));
            },
            buffer -> new MusicStreamChunkPayload(
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readList(buf -> buf.readByteArray(4096))
            )
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record MusicStreamStopPayload(int sessionId, MusicStopReason reason) implements CustomPacketPayload {
        public static final Type<MusicStreamStopPayload> TYPE = payloadType("music_stream_stop");
        public static final StreamCodec<RegistryFriendlyByteBuf, MusicStreamStopPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                buffer.writeVarInt(value.sessionId);
                buffer.writeEnum(value.reason);
            },
            buffer -> new MusicStreamStopPayload(buffer.readVarInt(), buffer.readEnum(MusicStopReason.class))
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payloadType(String path) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, path));
    }
}
