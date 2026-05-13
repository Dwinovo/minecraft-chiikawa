package com.dwinovo.chiikawa.music;

import com.dwinovo.chiikawa.entity.AbstractPet;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;

public final class MusicStreamSession {
    private final int sessionId;
    private final MusicTrack track;
    private final CachedOpusTrack cachedTrack;
    private final AbstractPet source;
    private final long startNanos;
    private final Set<ServerPlayer> listeners = new HashSet<>();
    private int nextFrameToSend;

    MusicStreamSession(int sessionId, MusicTrack track, CachedOpusTrack cachedTrack, AbstractPet source) {
        this.sessionId = sessionId;
        this.track = track;
        this.cachedTrack = cachedTrack;
        this.source = source;
        this.startNanos = System.nanoTime();
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

    public long startNanos() {
        return startNanos;
    }

    public int currentFrame() {
        long elapsedNanos = Math.max(0L, System.nanoTime() - startNanos);
        return (int) (elapsedNanos / 20_000_000L);
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
