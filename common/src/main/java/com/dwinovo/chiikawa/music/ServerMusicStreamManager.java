package com.dwinovo.chiikawa.music;

import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.network.MusicPayloads.MusicStreamChunkPayload;
import com.dwinovo.chiikawa.network.MusicPayloads.MusicStreamStartPayload;
import com.dwinovo.chiikawa.network.MusicPayloads.MusicStreamStopPayload;
import com.dwinovo.chiikawa.platform.Services;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class ServerMusicStreamManager {
    private final ServerMusicLibrary library;
    private final Map<Integer, MusicStreamSession> sessionsById = new HashMap<>();
    private final Map<Integer, MusicStreamSession> sessionsByEntity = new HashMap<>();
    private int nextSessionId = 1;

    public ServerMusicStreamManager(ServerMusicLibrary library) {
        this.library = library;
    }

    public Optional<MusicStreamSession> start(AbstractPet source, String trackId) {
        if (!library.config().enabled() || sessionsById.size() >= library.config().maxConcurrentSessions()) {
            return Optional.empty();
        }
        Optional<MusicTrack> track = library.getTrack(trackId);
        Optional<CachedOpusTrack> cached = library.loadCachedTrack(trackId);
        if (track.isEmpty() || cached.isEmpty()) {
            return Optional.empty();
        }

        stop(source, MusicStopReason.REPLACED);
        int sessionId = nextSessionId++;
        if (nextSessionId <= 0) {
            nextSessionId = 1;
        }
        MusicStreamSession session = new MusicStreamSession(sessionId, track.get(), cached.get(), source);
        sessionsById.put(sessionId, session);
        sessionsByEntity.put(source.getId(), session);
        return Optional.of(session);
    }

    public boolean isPlaying(AbstractPet source, String trackId) {
        MusicStreamSession session = sessionsByEntity.get(source.getId());
        return session != null && session.track().trackId().equals(trackId);
    }

    public void stop(AbstractPet source, MusicStopReason reason) {
        MusicStreamSession session = sessionsByEntity.remove(source.getId());
        if (session != null) {
            stopSession(session, reason);
        }
    }

    public void tick() {
        for (MusicStreamSession session : List.copyOf(sessionsById.values())) {
            tickSession(session);
        }
    }

    private void tickSession(MusicStreamSession session) {
        AbstractPet source = session.source();
        if (source.isRemoved() || !source.isAlive() || !(source.level() instanceof ServerLevel level)) {
            stopSession(session, MusicStopReason.SOURCE_REMOVED);
            return;
        }

        int currentFrame = session.currentFrame();
        if (currentFrame >= session.cachedTrack().frameCount()) {
            stopSession(session, MusicStopReason.FINISHED);
            if (source.getActivity() == PetActivity.PLAY_GUITAR) {
                source.setActivity(PetActivity.NONE);
            }
            return;
        }

        updateListeners(session, level, currentFrame);
        if (session.listeners().isEmpty() || currentFrame < session.nextFrameToSend()) {
            return;
        }

        List<byte[]> frames = List.copyOf(session.cachedTrack().slice(currentFrame, library.config().framesPerChunk()));
        if (frames.isEmpty()) {
            return;
        }
        MusicStreamChunkPayload payload = new MusicStreamChunkPayload(session.sessionId(), currentFrame, frames);
        for (ServerPlayer listener : List.copyOf(session.listeners())) {
            Services.NETWORK.sendToClient(listener, payload);
        }
        session.setNextFrameToSend(currentFrame + frames.size());
    }

    private void updateListeners(MusicStreamSession session, ServerLevel level, int currentFrame) {
        double radius = library.config().streamRadius();
        double radiusSq = radius * radius;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.distanceToSqr(session.source()) > radiusSq) {
                continue;
            }
            if (session.listeners().add(player)) {
                sendStart(session, player, currentFrame);
            }
        }

        for (ServerPlayer listener : List.copyOf(session.listeners())) {
            if (listener.isRemoved()
                    || listener.level() != level
                    || listener.isSpectator()
                    || listener.distanceToSqr(session.source()) > radiusSq) {
                session.listeners().remove(listener);
                Services.NETWORK.sendToClient(listener, new MusicStreamStopPayload(session.sessionId(), MusicStopReason.OUT_OF_RANGE));
            }
        }
    }

    private void sendStart(MusicStreamSession session, ServerPlayer player, int currentFrame) {
        CachedOpusTrack cached = session.cachedTrack();
        Services.NETWORK.sendToClient(player, new MusicStreamStartPayload(
            session.sessionId(),
            session.source().getId(),
            session.track().trackId(),
            session.track().title(),
            currentFrame,
            cached.sampleRate(),
            cached.channels(),
            cached.frameSamples(),
            library.config().framesPerChunk(),
            library.config().jitterBufferChunks(),
            library.config().streamRadius()
        ));
    }

    private void stopSession(MusicStreamSession session, MusicStopReason reason) {
        sessionsById.remove(session.sessionId());
        sessionsByEntity.remove(session.source().getId());
        if (session.source().getActivity() == PetActivity.PLAY_GUITAR) {
            session.source().setActivity(PetActivity.NONE);
        }
        MusicStreamStopPayload payload = new MusicStreamStopPayload(session.sessionId(), reason);
        for (ServerPlayer listener : List.copyOf(session.listeners())) {
            Services.NETWORK.sendToClient(listener, payload);
        }
        session.listeners().clear();
    }
}
