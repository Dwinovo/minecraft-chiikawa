package com.dwinovo.chiikawa.photo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PhotoChunkTest {
    private static final UUID PHOTO = UUID.randomUUID();

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static byte[] random(int count) {
        byte[] data = new byte[count];
        new Random(7).nextBytes(data);
        return data;
    }

    private static RegistryFriendlyByteBuf buffer() {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
    }

    @Test
    void aPhotoIsCutIntoPiecesNoBiggerThanAPacketTakes() {
        byte[] png = random(2 * PhotoChunk.MAX_BYTES + 5);

        List<PhotoChunk> chunks = PhotoChunk.split(PHOTO, png);

        assertEquals(3, chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            assertEquals(i, chunks.get(i).index());
            assertEquals(3, chunks.get(i).total());
            assertEquals(PHOTO, chunks.get(i).photo());
            assertTrue(chunks.get(i).bytes().length <= PhotoChunk.MAX_BYTES);
        }
        assertEquals(5, chunks.get(2).bytes().length);
    }

    @Test
    void aPhotoThatJustFitsIsOnePiece() {
        assertEquals(1, PhotoChunk.split(PHOTO, random(PhotoChunk.MAX_BYTES)).size());
        assertEquals(2, PhotoChunk.split(PHOTO, random(PhotoChunk.MAX_BYTES + 1)).size());
    }

    @Test
    void thePiecesPutBackTogetherAreThePhoto() {
        byte[] png = random(3 * PhotoChunk.MAX_BYTES - 17);
        ChunkAssembler assembler = ChunkAssembler.forPhoto();

        // Backwards, as a network may deliver them.
        List<PhotoChunk> chunks = PhotoChunk.split(PHOTO, png);
        for (int i = chunks.size() - 1; i >= 0; i--) {
            assembler.add(chunks.get(i).index(), chunks.get(i).total(), chunks.get(i).bytes());
        }

        assertArrayEquals(png, assembler.assemble());
    }

    @Test
    void aPieceSurvivesTheWire() {
        PhotoChunk chunk = PhotoChunk.split(PHOTO, random(100)).get(0);
        RegistryFriendlyByteBuf buffer = buffer();

        PhotoChunk.STREAM_CODEC.encode(buffer, chunk);
        PhotoChunk read = PhotoChunk.STREAM_CODEC.decode(buffer);

        assertEquals(chunk.photo(), read.photo());
        assertEquals(chunk.index(), read.index());
        assertEquals(chunk.total(), read.total());
        assertArrayEquals(chunk.bytes(), read.bytes());
    }

    @Test
    void aPieceBiggerThanAPieceMayBeIsNotReadAtAll() {
        RegistryFriendlyByteBuf largest = buffer();
        PhotoChunk.STREAM_CODEC.encode(largest, new PhotoChunk(PHOTO, 0, 1, new byte[PhotoChunk.MAX_BYTES]));
        assertEquals(PhotoChunk.MAX_BYTES, PhotoChunk.STREAM_CODEC.decode(largest).bytes().length);

        RegistryFriendlyByteBuf tooBig = buffer();
        // Written by hand: a well-behaved game never makes one this size.
        UUIDUtil.STREAM_CODEC.encode(tooBig, PHOTO);
        tooBig.writeVarInt(0);
        tooBig.writeVarInt(1);
        tooBig.writeVarInt(PhotoChunk.MAX_BYTES + 1);
        tooBig.writeBytes(new byte[PhotoChunk.MAX_BYTES + 1]);

        assertThrows(DecoderException.class, () -> PhotoChunk.STREAM_CODEC.decode(tooBig));
    }

    @Test
    void theWordThatThereIsNoSuchPhotoIsNotAPiece() {
        PhotoChunk missing = PhotoChunk.missing(PHOTO);

        assertTrue(missing.isMissing());
        assertFalse(PhotoChunk.split(PHOTO, random(10)).get(0).isMissing());
        RegistryFriendlyByteBuf buffer = buffer();
        PhotoChunk.STREAM_CODEC.encode(buffer, missing);
        assertTrue(PhotoChunk.STREAM_CODEC.decode(buffer).isMissing());
    }
}
