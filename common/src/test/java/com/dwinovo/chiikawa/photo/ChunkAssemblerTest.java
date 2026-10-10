package com.dwinovo.chiikawa.photo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.photo.ChunkAssembler.Result;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ChunkAssemblerTest {
    /** A whole of at most 100 bytes, in pieces of at most 10. */
    private static ChunkAssembler small() {
        return new ChunkAssembler(100, 10);
    }

    private static byte[] bytes(int count, int fill) {
        byte[] data = new byte[count];
        Arrays.fill(data, (byte) fill);
        return data;
    }

    @Test
    void piecesInOrderMakeTheWholeInOrder() {
        ChunkAssembler assembler = small();

        assertEquals(Result.ACCEPTED, assembler.add(0, 2, bytes(3, 1)));
        assertFalse(assembler.isComplete());
        assertEquals(Result.ACCEPTED, assembler.add(1, 2, bytes(2, 2)));

        assertTrue(assembler.isComplete());
        assertArrayEquals(new byte[] {1, 1, 1, 2, 2}, assembler.assemble());
    }

    @Test
    void piecesOutOfOrderMakeTheSameWhole() {
        ChunkAssembler assembler = small();

        assembler.add(2, 3, bytes(1, 3));
        assembler.add(0, 3, bytes(1, 1));
        assertFalse(assembler.isComplete());
        assembler.add(1, 3, bytes(1, 2));

        assertArrayEquals(new byte[] {1, 2, 3}, assembler.assemble());
    }

    @Test
    void aPieceSentTwiceCountsOnceAndTheFirstCopyStands() {
        ChunkAssembler assembler = small();
        assembler.add(0, 2, bytes(3, 1));

        assertEquals(Result.DUPLICATE, assembler.add(0, 2, bytes(3, 9)));
        assembler.add(1, 2, bytes(1, 2));

        assertArrayEquals(new byte[] {1, 1, 1, 2}, assembler.assemble());
    }

    @Test
    void aPhotoWithAPieceMissingIsNotComplete() {
        ChunkAssembler assembler = small();
        assembler.add(0, 3, bytes(1, 1));
        assembler.add(2, 3, bytes(1, 3));

        assertFalse(assembler.isComplete());
        assertThrows(IllegalStateException.class, assembler::assemble);
    }

    @Test
    void nothingIsCompleteBeforeTheFirstPiece() {
        assertFalse(small().isComplete());
    }

    @Test
    void piecesPastTheLimitAreTurnedAway() {
        ChunkAssembler assembler = new ChunkAssembler(10, 6);

        assertEquals(Result.ACCEPTED, assembler.add(0, 2, bytes(6, 1)));
        assertEquals(Result.TOO_BIG, assembler.add(1, 2, bytes(5, 1)));
        assertFalse(assembler.isComplete());
    }

    @Test
    void aPieceBiggerThanAPieceMayBeIsRejected() {
        assertEquals(Result.REJECTED, small().add(0, 1, bytes(11, 1)));
        assertEquals(Result.ACCEPTED, small().add(0, 1, bytes(10, 1)));
    }

    @Test
    void piecesThatDoNotMakeSenseAreRejected() {
        ChunkAssembler assembler = small();

        assertEquals(Result.REJECTED, assembler.add(0, 0, bytes(1, 1)), "no pieces in all");
        assertEquals(Result.REJECTED, assembler.add(-1, 2, bytes(1, 1)), "before the first");
        assertEquals(Result.REJECTED, assembler.add(2, 2, bytes(1, 1)), "past the last");
        assertEquals(Result.REJECTED, assembler.add(0, 2, new byte[0]), "empty");
    }

    @Test
    void aPieceThatDisagreesAboutHowManyThereAreIsRejected() {
        ChunkAssembler assembler = small();
        assembler.add(0, 2, bytes(1, 1));

        assertEquals(Result.REJECTED, assembler.add(1, 3, bytes(1, 2)));
        assertFalse(assembler.isComplete());
    }

    @Test
    void moreFullPiecesThanTheLimitCouldHoldAreRejected() {
        // 100 bytes in pieces of 10: at most 10 of them.
        assertEquals(Result.ACCEPTED, small().add(0, 10, bytes(1, 1)));
        assertEquals(Result.REJECTED, small().add(0, 11, bytes(1, 1)));
    }

    @Test
    void aPhotoTakesPiecesAsBigAsAPacketAndNoMoreThanAPhotoMayBe() {
        ChunkAssembler assembler = ChunkAssembler.forPhoto();
        int pieces = PhotoChunk.MAX_PHOTO_BYTES / PhotoChunk.MAX_BYTES + 1;

        assertEquals(Result.ACCEPTED, assembler.add(0, pieces, bytes(PhotoChunk.MAX_BYTES, 1)));
        assertEquals(Result.REJECTED, ChunkAssembler.forPhoto().add(0, 1, bytes(PhotoChunk.MAX_BYTES + 1, 1)));
        assertEquals(Result.REJECTED, ChunkAssembler.forPhoto().add(0, pieces + 1, bytes(1, 1)));
    }

    @Test
    void theCallerKeepsNoHoldOnWhatItHandedIn() {
        ChunkAssembler assembler = small();
        byte[] piece = bytes(2, 1);
        assembler.add(0, 1, piece);

        piece[0] = 9;

        assertArrayEquals(new byte[] {1, 1}, assembler.assemble());
    }
}
