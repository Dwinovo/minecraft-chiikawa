package com.dwinovo.chiikawa.music;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import io.github.jaredmdobson.concentus.OpusApplication;
import io.github.jaredmdobson.concentus.OpusEncoder;

public final class MusicImporter {
    private static final int FRAME_SAMPLES = 960;
    private static final int FRAME_BYTES = FRAME_SAMPLES * 2;
    private static final int MAX_OPUS_PACKET_BYTES = 4000;

    private MusicImporter() {
    }

    public static ImportResult importTrack(Path source, Path cacheFile, ChiikawaMusicConfig config)
        throws Exception {
        String extension = JvmAudioDecoder.extensionOf(source);
        if (!JvmAudioDecoder.supports(extension)) {
            throw new IOException("Unsupported audio format '" + extension + "' — please use MP3");
        }
        Files.createDirectories(cacheFile.getParent());
        CachedOpusTrack track;
        try (InputStream pcm = JvmAudioDecoder.decodeToPcm(source, config)) {
            track = encodePcmStream(pcm, config);
        }
        if (track.frameCount() == 0) {
            throw new IOException("No audio frames produced for " + source.getFileName());
        }
        CmsTrackFile.write(cacheFile, track);
        return new ImportResult(track.durationTicks(), track.frameCount());
    }

    private static CachedOpusTrack encodePcmStream(InputStream input, ChiikawaMusicConfig config) throws Exception {
        OpusEncoder encoder = new OpusEncoder(config.sampleRate(), config.channels(), OpusApplication.OPUS_APPLICATION_AUDIO);
        encoder.setBitrate(config.opusBitrate());
        List<byte[]> frames = new ArrayList<>();
        byte[] raw = new byte[FRAME_BYTES];
        short[] pcm = new short[FRAME_SAMPLES];
        byte[] opus = new byte[MAX_OPUS_PACKET_BYTES];

        while (true) {
            int read = readFrame(input, raw);
            if (read <= 0) {
                break;
            }
            if (read < raw.length) {
                Arrays.fill(raw, read, raw.length, (byte) 0);
            }
            for (int i = 0; i < FRAME_SAMPLES; i++) {
                int lo = raw[i * 2] & 0xFF;
                int hi = raw[i * 2 + 1];
                pcm[i] = (short) ((hi << 8) | lo);
            }
            int encoded = encoder.encode(pcm, 0, FRAME_SAMPLES, opus, 0, opus.length);
            frames.add(Arrays.copyOf(opus, encoded));
        }

        return new CachedOpusTrack(config.sampleRate(), config.channels(), FRAME_SAMPLES, List.copyOf(frames));
    }

    private static int readFrame(InputStream input, byte[] raw) throws IOException {
        int total = 0;
        while (total < raw.length) {
            int read = input.read(raw, total, raw.length - total);
            if (read < 0) {
                break;
            }
            total += read;
        }
        return total;
    }

    public record ImportResult(int durationTicks, int frameCount) {
    }
}
