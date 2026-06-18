package com.dwinovo.chiikawa.music;

import com.dwinovo.chiikawa.entity.AbstractPet;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;

public final class MusicStreamSession {
    /** Cap per-tick clock advance so a paused/frozen server (no ticks) doesn't skip the song on resume. */
    private static final long MAX_TICK_NANOS = 200_000_000L;
    private static final long FRAME_NANOS = 20_000_000L;

    private final int sessionId;
    private final MusicTrack track;
    private final CachedOpusTrack cachedTrack;
    private final AbstractPet source;
    private final Set<ServerPlayer> listeners = new HashSet<>();
    private long elapsedNanos;
    private long lastTickNanos;
    private int nextFrameToSend;

    MusicStreamSession(int sessionId, MusicTrack track, CachedOpusTrack cachedTrack, AbstractPet source) {
        this.sessionId = sessionId;
        this.track = track;
        this.cachedTrack = cachedTrack;
        this.source = source;
    }

    /**
     * Advances the playback clock by the real time since the previous tick, capped at
     * {@link #MAX_TICK_NANOS}. Driven by ticks (not absolute wall-clock) so that when the
     * server stops ticking — e.g. a single-player ESC pause — the playback head freezes
     * instead of leaping forward by the whole pause duration.
     */
    public void advanceClock(long nowNanos) {
        if (lastTickNanos != 0L) {
            long delta = Math.max(0L, nowNanos - lastTickNanos);
            elapsedNanos += Math.min(delta, MAX_TICK_NANOS);
        }
        lastTickNanos = nowNanos;
    }

    public int sessionId() {
        return sessionId;
    }

    public MusicTrack track() {
        return track;
    }

    public CachedOpusTrack cachedTrack() {
        return cachedTrack;
    }

    public AbstractPet source() {
        return source;
    }

    public int currentFrame() {
        return (int) (elapsedNanos / FRAME_NANOS);
    }

    public Set<ServerPlayer> listeners() {
        return listeners;
    }

    public int nextFrameToSend() {
        return nextFrameToSend;
    }

    public void setNextFrameToSend(int nextFrameToSend) {
        this.nextFrameToSend = nextFrameToSend;
    }
}
