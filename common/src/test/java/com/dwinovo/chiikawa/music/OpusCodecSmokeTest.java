package com.dwinovo.chiikawa.music;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.jaredmdobson.concentus.OpusApplication;
import io.github.jaredmdobson.concentus.OpusDecoder;
import io.github.jaredmdobson.concentus.OpusEncoder;
import org.junit.jupiter.api.Test;

class OpusCodecSmokeTest {
    @Test
    void concentusEncodesAndDecodesOneTwentyMillisecondFrame() throws Exception {
        int sampleRate = 48_000;
        int frameSamples = 960;
        short[] pcm = new short[frameSamples];
        for (int i = 0; i < frameSamples; i++) {
            pcm[i] = (short) (Math.sin(i / 12.0D) * 8_000.0D);
        }

        OpusEncoder encoder = new OpusEncoder(sampleRate, 1, OpusApplication.OPUS_APPLICATION_AUDIO);
        encoder.setBitrate(48_000);
        byte[] opus = new byte[4096];
        int opusLength = encoder.encode(pcm, 0, frameSamples, opus, 0, opus.length);

        OpusDecoder decoder = new OpusDecoder(sampleRate, 1);
        short[] decoded = new short[frameSamples];
        int decodedSamples = decoder.decode(opus, 0, opusLength, decoded, 0, frameSamples, false);

        assertTrue(opusLength > 0);
        assertTrue(decodedSamples > 0);
    }
}
