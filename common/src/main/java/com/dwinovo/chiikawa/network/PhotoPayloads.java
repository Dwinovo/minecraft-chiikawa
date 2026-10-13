package com.dwinovo.chiikawa.network;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.photo.FilmRoll;
import com.dwinovo.chiikawa.photo.PhotoChunk;
import com.dwinovo.chiikawa.photo.PhotoData;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Everything said about photos: the server asking a player's game to take one, the game
 * sending it back in pieces, and the pieces coming the other way when a photo is looked at.
 */
public final class PhotoPayloads {
    public static final ResourceLocation TAKE_PHOTO = payloadId("take_photo");
    public static final ResourceLocation UPLOAD_PHOTO = payloadId("upload_photo");
    public static final ResourceLocation REQUEST_PHOTO = payloadId("request_photo");
    public static final ResourceLocation DOWNLOAD_PHOTO = payloadId("download_photo");
    public static final ResourceLocation OPEN_ALBUM = payloadId("open_album");

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
            implements MusicPayloads.Payload {
        public static TakePhotoPayload read(FriendlyByteBuf buffer) {
            return new TakePhotoPayload(buffer.readUUID(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt());
        }

        @Override
        public ResourceLocation id() {
            return TAKE_PHOTO;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeUUID(photo);
            buffer.writeVarInt(viewpoint);
            buffer.writeVarInt(width);
            buffer.writeVarInt(height);
            buffer.writeVarInt(fov);
        }
    }

    /** One piece of a photo the game was asked to take, on its way to the server. */
    public record UploadPhotoPayload(PhotoChunk chunk) implements MusicPayloads.Payload {
        public static UploadPhotoPayload read(FriendlyByteBuf buffer) {
            return new UploadPhotoPayload(PhotoChunk.read(buffer));
        }

        @Override
        public ResourceLocation id() {
            return UPLOAD_PHOTO;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            chunk.write(buffer);
        }
    }

    /** A game asking for a photo it has not got. */
    public record RequestPhotoPayload(UUID photo) implements MusicPayloads.Payload {
        public static RequestPhotoPayload read(FriendlyByteBuf buffer) {
            return new RequestPhotoPayload(buffer.readUUID());
        }

        @Override
        public ResourceLocation id() {
            return REQUEST_PHOTO;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeUUID(photo);
        }
    }

    /** One piece of a photo that was asked for, or the word that there is no such photo. */
    public record DownloadPhotoPayload(PhotoChunk chunk) implements MusicPayloads.Payload {
        public static DownloadPhotoPayload read(FriendlyByteBuf buffer) {
            return new DownloadPhotoPayload(PhotoChunk.read(buffer));
        }

        @Override
        public ResourceLocation id() {
            return DOWNLOAD_PHOTO;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            chunk.write(buffer);
        }
    }

    /** Opens the album screen on the shots of a roll. The pictures are not in it: the game fetches those by id. */
    public record OpenAlbumPayload(List<PhotoData> shots) implements MusicPayloads.Payload {
        public static OpenAlbumPayload read(FriendlyByteBuf buffer) {
            int size = buffer.readVarInt();
            if (size > FilmRoll.MOST_EXPOSURES) {
                throw new DecoderException("A roll of " + size + " shots is bigger than the allowed " + FilmRoll.MOST_EXPOSURES);
            }
            List<PhotoData> shots = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                shots.add(PhotoData.read(buffer));
            }
            return new OpenAlbumPayload(shots);
        }

        @Override
        public ResourceLocation id() {
            return OPEN_ALBUM;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(shots.size());
            for (PhotoData shot : shots) {
                shot.write(buffer);
            }
        }
    }

    private static ResourceLocation payloadId(String path) {
        return new ResourceLocation(Constants.MOD_ID, path);
    }
}
