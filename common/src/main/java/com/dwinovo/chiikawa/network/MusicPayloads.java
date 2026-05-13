package com.dwinovo.chiikawa.network;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.music.MusicStopReason;
import com.dwinovo.chiikawa.music.MusicTrackView;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public final class MusicPayloads {
    public static final ResourceLocation MUSIC_CATALOG = payloadId("music_catalog");
    public static final ResourceLocation MUSIC_BOX_SELECT_TRACK = payloadId("music_box_select_track");
    public static final ResourceLocation MUSIC_CATALOG_REQUEST = payloadId("music_catalog_request");
    public static final ResourceLocation MUSIC_STREAM_START = payloadId("music_stream_start");
    public static final ResourceLocation MUSIC_STREAM_CHUNK = payloadId("music_stream_chunk");
    public static final ResourceLocation MUSIC_STREAM_STOP = payloadId("music_stream_stop");

    private MusicPayloads() {
    }

    public interface Payload {
        ResourceLocation id();

        void write(FriendlyByteBuf buffer);
    }

    public record MusicCatalogPayload(int handIndex, List<MusicTrackView> tracks, boolean openScreen) implements Payload {
        public static MusicCatalogPayload read(FriendlyByteBuf buffer) {
            int handIndex = buffer.readVarInt();
            int size = buffer.readVarInt();
            List<MusicTrackView> tracks = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                tracks.add(MusicTrackView.read(buffer));
            }
            return new MusicCatalogPayload(handIndex, tracks, buffer.readBoolean());
        }

        @Override
        public ResourceLocation id() {
            return MUSIC_CATALOG;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(handIndex);
            buffer.writeVarInt(tracks.size());
            for (MusicTrackView track : tracks) {
                track.write(buffer);
            }
            buffer.writeBoolean(openScreen);
        }
    }

    public record MusicBoxSelectTrackPayload(int handIndex, String trackId) implements Payload {
        public static MusicBoxSelectTrackPayload read(FriendlyByteBuf buffer) {
            return new MusicBoxSelectTrackPayload(buffer.readVarInt(), buffer.readUtf());
        }

        @Override
        public ResourceLocation id() {
            return MUSIC_BOX_SELECT_TRACK;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(handIndex);
            buffer.writeUtf(trackId);
        }
    }

    public record MusicCatalogRequestPayload(int handIndex, boolean rescan) implements Payload {
        public static MusicCatalogRequestPayload read(FriendlyByteBuf buffer) {
            return new MusicCatalogRequestPayload(buffer.readVarInt(), buffer.readBoolean());
        }

        @Override
        public ResourceLocation id() {
            return MUSIC_CATALOG_REQUEST;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(handIndex);
            buffer.writeBoolean(rescan);
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
    ) implements Payload {
        public static MusicStreamStartPayload read(FriendlyByteBuf buffer) {
            return new MusicStreamStartPayload(
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
            );
        }

        @Override
        public ResourceLocation id() {
            return MUSIC_STREAM_START;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(sessionId);
            buffer.writeVarInt(entityId);
            buffer.writeUtf(trackId);
            buffer.writeUtf(title);
            buffer.writeVarInt(startFrame);
            buffer.writeVarInt(sampleRate);
            buffer.writeVarInt(channels);
            buffer.writeVarInt(frameSamples);
            buffer.writeVarInt(framesPerChunk);
            buffer.writeVarInt(jitterBufferChunks);
            buffer.writeVarInt(streamRadius);
        }
    }

    public record MusicStreamChunkPayload(int sessionId, int frameStart, List<byte[]> frames) implements Payload {
        public static MusicStreamChunkPayload read(FriendlyByteBuf buffer) {
            int sessionId = buffer.readVarInt();
            int frameStart = buffer.readVarInt();
            int size = buffer.readVarInt();
            List<byte[]> frames = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                frames.add(buffer.readByteArray(4096));
            }
            return new MusicStreamChunkPayload(sessionId, frameStart, frames);
        }

        @Override
        public ResourceLocation id() {
            return MUSIC_STREAM_CHUNK;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(sessionId);
            buffer.writeVarInt(frameStart);
            buffer.writeVarInt(frames.size());
            for (byte[] frame : frames) {
                buffer.writeByteArray(frame);
            }
        }
    }

    public record MusicStreamStopPayload(int sessionId, MusicStopReason reason) implements Payload {
        public static MusicStreamStopPayload read(FriendlyByteBuf buffer) {
            return new MusicStreamStopPayload(buffer.readVarInt(), buffer.readEnum(MusicStopReason.class));
        }

        @Override
        public ResourceLocation id() {
            return MUSIC_STREAM_STOP;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(sessionId);
            buffer.writeEnum(reason);
        }
    }

    private static ResourceLocation payloadId(String path) {
        return new ResourceLocation(Constants.MOD_ID, path);
    }
}
