package com.dwinovo.chiikawa.network;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.photo.FilmRoll;
import com.dwinovo.chiikawa.photo.PhotoChunk;
import com.dwinovo.chiikawa.photo.PhotoData;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Everything said about photos: the server asking a player's game to take one, the game
 * sending it back in pieces, and the pieces coming the other way when a photo is looked at.
 */
public final class PhotoPayloads {
    private PhotoPayloads() {
    }

    /**
     * Asks the game to draw a picture from an entity's eyes and send it back.
     *
     * @param photo the id the picture is to be sent back under
     * @param viewpoint the entity id of whose eyes it is drawn from
     * @param width how wide the photo is, in pixels
     * @param height how tall
     * @param fov how wide the lens sees, in degrees from top to bottom
     */
    public record TakePhotoPayload(UUID photo, int viewpoint, int width, int height, int fov)
            implements CustomPacketPayload {
        public static final Type<TakePhotoPayload> TYPE = payloadType("take_photo");
        public static final StreamCodec<RegistryFriendlyByteBuf, TakePhotoPayload> STREAM_CODEC =
            StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, TakePhotoPayload::photo,
                ByteBufCodecs.VAR_INT, TakePhotoPayload::viewpoint,
                ByteBufCodecs.VAR_INT, TakePhotoPayload::width,
                ByteBufCodecs.VAR_INT, TakePhotoPayload::height,
                ByteBufCodecs.VAR_INT, TakePhotoPayload::fov,
                TakePhotoPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** One piece of a photo the game was asked to take, on its way to the server. */
    public record UploadPhotoPayload(PhotoChunk chunk) implements CustomPacketPayload {
        public static final Type<UploadPhotoPayload> TYPE = payloadType("upload_photo");
        public static final StreamCodec<RegistryFriendlyByteBuf, UploadPhotoPayload> STREAM_CODEC =
            StreamCodec.composite(PhotoChunk.STREAM_CODEC, UploadPhotoPayload::chunk, UploadPhotoPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** A game asking for a photo it has not got. */
    public record RequestPhotoPayload(UUID photo) implements CustomPacketPayload {
        public static final Type<RequestPhotoPayload> TYPE = payloadType("request_photo");
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestPhotoPayload> STREAM_CODEC =
            StreamCodec.composite(UUIDUtil.STREAM_CODEC, RequestPhotoPayload::photo, RequestPhotoPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** One piece of a photo that was asked for, or the word that there is no such photo. */
    public record DownloadPhotoPayload(PhotoChunk chunk) implements CustomPacketPayload {
        public static final Type<DownloadPhotoPayload> TYPE = payloadType("download_photo");
        public static final StreamCodec<RegistryFriendlyByteBuf, DownloadPhotoPayload> STREAM_CODEC =
            StreamCodec.composite(PhotoChunk.STREAM_CODEC, DownloadPhotoPayload::chunk, DownloadPhotoPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Opens the album screen on the shots of a roll. The pictures are not in it: the game fetches those by id. */
    public record OpenAlbumPayload(List<PhotoData> shots) implements CustomPacketPayload {
        public static final Type<OpenAlbumPayload> TYPE = payloadType("open_album");
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenAlbumPayload> STREAM_CODEC =
            StreamCodec.composite(PhotoData.STREAM_CODEC.apply(ByteBufCodecs.list(FilmRoll.MOST_EXPOSURES)),
                OpenAlbumPayload::shots, OpenAlbumPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payloadType(String path) {
        return new CustomPacketPayload.Type<>(new ResourceLocation(Constants.MOD_ID, path));
    }
}
