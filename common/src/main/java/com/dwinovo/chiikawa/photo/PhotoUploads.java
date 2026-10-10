package com.dwinovo.chiikawa.photo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The photos the server is waiting for, and the pieces that have come in for them. A photo
 * is only accepted if the server asked for it: from the player it asked, at the size it
 * asked for, within a minute, and no bigger than a photo can be. Whatever else arrives is
 * dropped without a word, so a player cannot fill the server with files nobody wanted or
 * cancel somebody else's photo by sending a piece for it.
 *
 * <p>Time is the server's tick count, handed in, so none of this needs a server.
 */
public final class PhotoUploads {
    /** How long a photo is waited for, in ticks: a minute. */
    public static final long WAIT_TICKS = 1200;

    /** What the server asked for. */
    public record Request(UUID photo, UUID owner, int width, int height) {
    }

    /** A photo whose pieces are all in, not yet looked at. */
    public record Arrived(Request request, byte[] png) {
    }

    /** Who hears how a wait ended. */
    public interface Listener {
        /** The photo is in the library. */
        void stored(UUID photo);

        /** The photo is not coming, or came wrong. Nothing was kept. */
        void failed(UUID photo);
    }

    private static final class Waiting {
        private final Request request;
        private final long expires;
        private final Listener listener;
        private final ChunkAssembler pieces = ChunkAssembler.forPhoto();

        private Waiting(Request request, long expires, Listener listener) {
            this.request = request;
            this.expires = expires;
            this.listener = listener;
        }
    }

    private final Map<UUID, Waiting> waiting = new HashMap<>();

    /** Starts waiting for a photo. */
    public void expect(Request request, long now, Listener listener) {
        waiting.put(request.photo(), new Waiting(request, now + WAIT_TICKS, listener));
    }

    public boolean isWaitingFor(UUID photo) {
        return waiting.containsKey(photo);
    }

    /**
     * Takes in one piece.
     *
     * @param sender who sent it
     * @return the photo, once this was the last piece it was missing
     */
    public Optional<Arrived> accept(UUID photo, UUID sender, int index, int total, byte[] data, long now) {
        Waiting wait = waiting.get(photo);
        if (wait == null || now >= wait.expires || !wait.request.owner().equals(sender)) {
            return Optional.empty();
        }
        if (wait.pieces.add(index, total, data) == ChunkAssembler.Result.TOO_BIG) {
            // Past the most a photo can be: not a photo, and not worth waiting for.
            waiting.remove(photo);
            wait.listener.failed(photo);
            return Optional.empty();
        }
        return wait.pieces.isComplete()
            ? Optional.of(new Arrived(wait.request, wait.pieces.assemble()))
            : Optional.empty();
    }

    /** Ends the wait for a photo that has been looked at, and tells whoever was waiting for it. */
    public void finish(UUID photo, boolean stored) {
        Waiting wait = waiting.remove(photo);
        if (wait != null) {
            if (stored) {
                wait.listener.stored(photo);
            } else {
                wait.listener.failed(photo);
            }
        }
    }

    /** Gives up on every photo that has been waited for long enough. */
    public void expire(long now) {
        List<Waiting> lapsed = new ArrayList<>();
        for (Iterator<Waiting> each = waiting.values().iterator(); each.hasNext(); ) {
            Waiting wait = each.next();
            if (now >= wait.expires) {
                lapsed.add(wait);
                each.remove();
            }
        }
        lapsed.forEach(wait -> wait.listener.failed(wait.request.photo()));
    }
}
