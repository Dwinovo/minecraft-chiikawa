package com.dwinovo.chiikawa.photo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PhotoUploadsTest {
    private static final UUID PHOTO = UUID.randomUUID();
    private static final UUID OWNER = UUID.randomUUID();
    private static final UUID STRANGER = UUID.randomUUID();
    private static final PhotoUploads.Request REQUEST = new PhotoUploads.Request(PHOTO, OWNER, 32, 24);

    /** What the listener was told, in order. */
    private final List<String> heard = new ArrayList<>();
    private final PhotoUploads.Listener listener = new PhotoUploads.Listener() {
        @Override
        public void stored(UUID photo) {
            heard.add("stored " + photo);
        }

        @Override
        public void failed(UUID photo) {
            heard.add("failed " + photo);
        }
    };
    private final PhotoUploads uploads = new PhotoUploads();

    private static byte[] bytes(int count) {
        byte[] data = new byte[count];
        Arrays.fill(data, (byte) 5);
        return data;
    }

    @Test
    void thePiecesOfAPhotoThatWasAskedForComeBackAsOne() {
        uploads.expect(REQUEST, 100, listener);

        assertTrue(uploads.accept(PHOTO, OWNER, 0, 2, bytes(3), 110).isEmpty());
        Optional<PhotoUploads.Arrived> arrived = uploads.accept(PHOTO, OWNER, 1, 2, bytes(2), 111);

        assertTrue(arrived.isPresent());
        assertEquals(REQUEST, arrived.get().request());
        assertArrayEquals(bytes(5), arrived.get().png());
    }

    @Test
    void aPhotoNobodyAskedForIsDropped() {
        assertTrue(uploads.accept(UUID.randomUUID(), OWNER, 0, 1, bytes(3), 0).isEmpty());
        assertFalse(uploads.isWaitingFor(PHOTO));
    }

    @Test
    void aPhotoFromAnotherPlayerIsDroppedAndDoesNotSpoilTheWait() {
        uploads.expect(REQUEST, 0, listener);

        assertTrue(uploads.accept(PHOTO, STRANGER, 0, 1, bytes(3), 5).isEmpty());

        assertTrue(uploads.isWaitingFor(PHOTO));
        assertTrue(uploads.accept(PHOTO, OWNER, 0, 1, bytes(3), 6).isPresent(), "the owner's own is still taken");
        assertEquals(List.of(), heard, "nobody was told anything went wrong");
    }

    @Test
    void aPhotoThatGrowsPastTheLimitFailsAndIsForgotten() {
        uploads.expect(REQUEST, 0, listener);
        int pieces = PhotoChunk.MAX_PHOTO_BYTES / PhotoChunk.MAX_BYTES + 1;

        for (int index = 0; index < pieces; index++) {
            uploads.accept(PHOTO, OWNER, index, pieces, bytes(PhotoChunk.MAX_BYTES), 1);
        }

        assertEquals(List.of("failed " + PHOTO), heard);
        assertFalse(uploads.isWaitingFor(PHOTO));
        assertTrue(uploads.accept(PHOTO, OWNER, 0, 1, bytes(3), 2).isEmpty());
    }

    @Test
    void aPhotoThatNeverComesIsGivenUpOnAfterAMinute() {
        uploads.expect(REQUEST, 100, listener);

        uploads.expire(100 + PhotoUploads.WAIT_TICKS - 1);
        assertEquals(List.of(), heard);
        assertTrue(uploads.isWaitingFor(PHOTO));

        uploads.expire(100 + PhotoUploads.WAIT_TICKS);
        assertEquals(List.of("failed " + PHOTO), heard);
        assertFalse(uploads.isWaitingFor(PHOTO));
    }

    @Test
    void aPieceThatArrivesAfterTheMinuteIsDroppedEvenBeforeAnyoneNoticedItLapsed() {
        uploads.expect(REQUEST, 0, listener);

        assertTrue(uploads.accept(PHOTO, OWNER, 0, 1, bytes(3), PhotoUploads.WAIT_TICKS).isEmpty());
    }

    @Test
    void aWaitIsToldHowItEndedOnlyOnce() {
        uploads.expect(REQUEST, 0, listener);

        uploads.finish(PHOTO, true);
        uploads.finish(PHOTO, false);
        uploads.expire(1_000_000);

        assertEquals(List.of("stored " + PHOTO), heard);
    }

    @Test
    void aPhotoThatCameWrongFails() {
        uploads.expect(REQUEST, 0, listener);

        uploads.finish(PHOTO, false);

        assertEquals(List.of("failed " + PHOTO), heard);
        assertFalse(uploads.isWaitingFor(PHOTO));
    }

    @Test
    void severalPhotosAreWaitedForApart() {
        UUID other = UUID.randomUUID();
        uploads.expect(REQUEST, 0, listener);
        uploads.expect(new PhotoUploads.Request(other, OWNER, 32, 24), 500, listener);

        uploads.expire(PhotoUploads.WAIT_TICKS);

        assertEquals(List.of("failed " + PHOTO), heard);
        assertTrue(uploads.isWaitingFor(other));
    }
}
