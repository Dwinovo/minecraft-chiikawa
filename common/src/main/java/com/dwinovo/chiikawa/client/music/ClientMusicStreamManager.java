package com.dwinovo.chiikawa.client.music;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.music.MusicStopReason;
import com.dwinovo.chiikawa.network.MusicPayloads.MusicStreamChunkPayload;
import com.dwinovo.chiikawa.network.MusicPayloads.MusicStreamStartPayload;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import io.github.jaredmdobson.concentus.OpusDecoder;
import io.github.jaredmdobson.concentus.OpusException;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;

public final class ClientMusicStreamManager {
    private static final Map<Integer, ClientSession> SESSIONS = new HashMap<>();
    private static final int MAX_QUEUED_BUFFERS = 24;
    private static final int MISSING_ENTITY_GRACE_TICKS = 100;
    private static final SoundSource MUSIC_BOX_SOUND_SOURCE = SoundSource.RECORDS;

    private ClientMusicStreamManager() {
    }

    public static void start(MusicStreamStartPayload payload) {
        stop(payload.sessionId(), MusicStopReason.REPLACED);
        try {
            ClientSession session = new ClientSession(payload);
            SESSIONS.put(payload.sessionId(), session);
        } catch (Exception ex) {
            Constants.LOG.warn("[chiikawa-music] Failed to start client stream {}", payload.sessionId(), ex);
        }
    }

    public static void acceptChunk(MusicStreamChunkPayload payload) {
        ClientSession session = SESSIONS.get(payload.sessionId());
        if (session != null) {
            session.accept(payload.frameStart(), payload.frames());
        }
    }

    public static void stop(int sessionId, MusicStopReason reason) {
        ClientSession session = SESSIONS.remove(sessionId);
        if (session != null) {
            session.close();
        }
    }

    public static void tick() {
        for (ClientSession session : List.copyOf(SESSIONS.values())) {
            if (!session.tick()) {
                SESSIONS.remove(session.sessionId);
                session.close();
            }
        }
    }

    private static final class ClientSession {
        private final int sessionId;
        private final int entityId;
        private final int source;
        private final OpusDecoder decoder;
        private final int sampleRate;
        private final int channels;
        private final int frameSamples;
        private final int framesPerChunk;
        private final int jitterBufferFrames;
        private final TreeMap<Integer, List<byte[]>> pendingChunks = new TreeMap<>();
        private int nextDecodeFrame;
        private int queuedBuffers;
        private int missingEntityTicks;
        private boolean started;

        ClientSession(MusicStreamStartPayload payload) throws OpusException {
            this.sessionId = payload.sessionId();
            this.entityId = payload.entityId();
            this.sampleRate = payload.sampleRate();
            this.channels = payload.channels();
            this.frameSamples = payload.frameSamples();
            this.framesPerChunk = payload.framesPerChunk();
            this.jitterBufferFrames = Math.max(1, payload.jitterBufferChunks()) * Math.max(1, framesPerChunk);
            this.nextDecodeFrame = Math.max(0, payload.startFrame());
            this.decoder = new OpusDecoder(sampleRate, channels);
            this.source = AL10.alGenSources();
            updateVolume();
            AL10.alSourcef(source, AL10.AL_REFERENCE_DISTANCE, 4.0F);
            AL10.alSourcef(source, AL10.AL_MAX_DISTANCE, Math.max(8.0F, payload.streamRadius()));
            AL10.alSourcef(source, AL10.AL_ROLLOFF_FACTOR, 1.0F);
            AL10.alSourcei(source, AL10.AL_SOURCE_RELATIVE, AL10.AL_FALSE);
        }

        void accept(int frameStart, List<byte[]> frames) {
            if (frames == null || frames.isEmpty()) {
                return;
            }
            pendingChunks.put(frameStart, List.copyOf(frames));
        }

        boolean tick() {
            Entity entity = sourceEntity();
            if (entity == null || entity.isRemoved()) {
                missingEntityTicks++;
                if (missingEntityTicks > MISSING_ENTITY_GRACE_TICKS) {
                    return false;
                }
            } else {
                missingEntityTicks = 0;
                updatePosition(entity);
            }

            updateVolume();
            releaseProcessedBuffers();
            queueAvailableFrames();

            int state = AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE);
            int queued = AL10.alGetSourcei(source, AL10.AL_BUFFERS_QUEUED);
            if (!started) {
                if (queued >= Math.max(1, jitterBufferFrames / Math.max(1, framesPerChunk))) {
                    AL10.alSourcePlay(source);
                    started = true;
                }
            } else if (queued > 0 && state != AL10.AL_PLAYING) {
                AL10.alSourcePlay(source);
            }
            return true;
        }

        private Entity sourceEntity() {
            Minecraft minecraft = Minecraft.getInstance();
            return minecraft.level == null ? null : minecraft.level.getEntity(entityId);
        }

        private void updatePosition(Entity entity) {
            Vec3 pos = entity.position();
            AL10.alSource3f(source, AL10.AL_POSITION, (float) pos.x, (float) pos.y, (float) pos.z);
            AL10.alSource3f(source, AL10.AL_VELOCITY, 0.0F, 0.0F, 0.0F);
        }

        private void updateVolume() {
            Minecraft minecraft = Minecraft.getInstance();
            AL10.alSourcef(source, AL10.AL_GAIN,
                minecraft.options.getFinalSoundSourceVolume(MUSIC_BOX_SOUND_SOURCE));
        }

        private void releaseProcessedBuffers() {
            int processed = AL10.alGetSourcei(source, AL10.AL_BUFFERS_PROCESSED);
            while (processed-- > 0) {
                int buffer = AL10.alSourceUnqueueBuffers(source);
                if (buffer != 0) {
                    AL10.alDeleteBuffers(buffer);
                    queuedBuffers = Math.max(0, queuedBuffers - 1);
                }
            }
        }

        private void queueAvailableFrames() {
            while (queuedBuffers < MAX_QUEUED_BUFFERS) {
                Map.Entry<Integer, List<byte[]>> entry = pendingChunks.firstEntry();
                if (entry == null) {
                    return;
                }
                int frameStart = entry.getKey();
                if (frameStart + entry.getValue().size() <= nextDecodeFrame) {
                    pendingChunks.pollFirstEntry();
                    continue;
                }
                if (frameStart > nextDecodeFrame) {
                    nextDecodeFrame = frameStart;
                }

                List<byte[]> frames = pendingChunks.pollFirstEntry().getValue();
                for (int i = Math.max(0, nextDecodeFrame - frameStart); i < frames.size(); i++) {
                    queueFrame(frames.get(i));
                    nextDecodeFrame++;
                    if (queuedBuffers >= MAX_QUEUED_BUFFERS) {
                        if (i + 1 < frames.size()) {
                            pendingChunks.put(nextDecodeFrame, frames.subList(i + 1, frames.size()));
                        }
                        return;
                    }
                }
            }
        }

        private void queueFrame(byte[] opusFrame) {
            try {
                short[] pcm = new short[frameSamples * channels];
                int decoded = decoder.decode(opusFrame, 0, opusFrame.length, pcm, 0, frameSamples, false);
                if (decoded <= 0) {
                    return;
                }
                ShortBuffer data = BufferUtils.createShortBuffer(decoded * channels);
                data.put(pcm, 0, decoded * channels);
                data.flip();
                int buffer = AL10.alGenBuffers();
                int format = channels == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;
                AL10.alBufferData(buffer, format, data, sampleRate);
                AL10.alSourceQueueBuffers(source, buffer);
                queuedBuffers++;
            } catch (OpusException ex) {
                Constants.LOG.warn("[chiikawa-music] Failed to decode Opus frame for session {}", sessionId, ex);
            }
        }

        void close() {
            AL10.alSourceStop(source);
            int queued = AL10.alGetSourcei(source, AL10.AL_BUFFERS_QUEUED);
            while (queued-- > 0) {
                int buffer = AL10.alSourceUnqueueBuffers(source);
                if (buffer != 0) {
                    AL10.alDeleteBuffers(buffer);
                }
            }
            AL10.alDeleteSources(source);
            pendingChunks.clear();
            queuedBuffers = 0;
        }
    }
}
