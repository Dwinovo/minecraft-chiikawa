package com.dwinovo.chiikawa.music;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Locale;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javazoom.spi.mpeg.sampled.convert.MpegFormatConversionProvider;
import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader;

/**
 * Pure-Java decoder for the common audio formats, producing 16-bit signed little-endian
 * <em>mono</em> PCM at the configured sample rate — the same byte layout FFmpeg used to emit,
 * so the Opus encoding path is unchanged.
 *
 * <p>MP3 is decoded by instantiating the mp3spi/JLayer providers <em>directly</em> rather than via
 * {@link AudioSystem}'s ServiceLoader lookup: under NeoForge the bundled library sits on the module
 * path, where {@code META-INF/services} SPI registration is ignored (Fabric puts it on the
 * classpath, which is why discovery worked there). WAV is handled by the JDK's own readers, which
 * live in the boot module layer and are found on every loader. This removes the external FFmpeg
 * dependency for the formats that cover ~95% of use.
 */
public final class JvmAudioDecoder {
    private JvmAudioDecoder() {
    }

    /** Formats we can decode without FFmpeg. */
    public static boolean supports(String extension) {
        return extension.equals("mp3") || extension.equals("wav");
    }

    /**
     * Decodes {@code source} into a stream of s16le mono PCM at {@code config.sampleRate()},
     * truncated to {@code config.maxTrackSeconds()}.
     */
    public static InputStream decodeToPcm(Path source, ChiikawaMusicConfig config)
            throws IOException, UnsupportedAudioFileException {
        String extension = extensionOf(source);
        if (extension.equals("mp3")) {
            return decodeMp3(source, config);
        }
        return decodeWithAudioSystem(source, config);
    }

    /** MP3 via mp3spi providers instantiated directly (no ServiceLoader / module-path lookup). */
    private static InputStream decodeMp3(Path source, ChiikawaMusicConfig config)
            throws IOException, UnsupportedAudioFileException {
        File file = source.toFile();
        AudioInputStream rawIn = new MpegAudioFileReader().getAudioInputStream(file);
        try (rawIn) {
            AudioFormat base = rawIn.getFormat();
            float sourceRate = base.getSampleRate();
            int channels = Math.max(1, base.getChannels());
            if (sourceRate <= 0) {
                throw new IOException("Unknown source sample rate for " + source.getFileName());
            }
            AudioFormat pcmFormat = pcmFormat(sourceRate, channels);
            try (AudioInputStream pcmIn = new MpegFormatConversionProvider().getAudioInputStream(pcmFormat, rawIn)) {
                return toMonoStream(pcmIn, config, sourceRate, channels);
            }
        }
    }

    /** WAV (and anything else the JDK natively reads) via the platform AudioSystem. */
    private static InputStream decodeWithAudioSystem(Path source, ChiikawaMusicConfig config)
            throws IOException, UnsupportedAudioFileException {
        try (AudioInputStream rawIn = AudioSystem.getAudioInputStream(source.toFile())) {
            AudioFormat base = rawIn.getFormat();
            float sourceRate = base.getSampleRate();
            int channels = Math.max(1, base.getChannels());
            if (sourceRate <= 0) {
                throw new IOException("Unknown source sample rate for " + source.getFileName());
            }
            AudioFormat pcmFormat = pcmFormat(sourceRate, channels);
            try (AudioInputStream pcmIn = AudioSystem.getAudioInputStream(pcmFormat, rawIn)) {
                return toMonoStream(pcmIn, config, sourceRate, channels);
            }
        }
    }

    private static AudioFormat pcmFormat(float sampleRate, int channels) {
        // Signed 16-bit little-endian PCM at the source rate; we resample to the target rate
        // ourselves so we don't depend on a SampleRateConversionProvider being present.
        return new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sampleRate, 16, channels, channels * 2, sampleRate, false);
    }

    private static InputStream toMonoStream(AudioInputStream pcmIn, ChiikawaMusicConfig config, float sourceRate, int channels)
            throws IOException {
        long maxBytes = (long) (sourceRate * config.maxTrackSeconds()) * channels * 2L;
        byte[] pcm = readCapped(pcmIn, maxBytes);
        short[] mono = downmixToMono(pcm, channels);
        short[] resampled = resampleLinear(mono, sourceRate, config.sampleRate());
        return new ByteArrayInputStream(toLittleEndian(resampled));
    }

    private static byte[] readCapped(InputStream in, long maxBytes) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[16384];
        long total = 0;
        while (total < maxBytes) {
            int want = (int) Math.min(buffer.length, maxBytes - total);
            int read = in.read(buffer, 0, want);
            if (read < 0) {
                break;
            }
            out.write(buffer, 0, read);
            total += read;
        }
        return out.toByteArray();
    }

    /** Interprets interleaved s16le samples and averages channels down to mono. */
    private static short[] downmixToMono(byte[] pcm, int channels) {
        int frames = (pcm.length / 2) / channels;
        short[] mono = new short[frames];
        for (int f = 0; f < frames; f++) {
            int sum = 0;
            for (int c = 0; c < channels; c++) {
                int idx = (f * channels + c) * 2;
                int lo = pcm[idx] & 0xFF;
                int hi = pcm[idx + 1]; // sign-extended
                sum += (short) ((hi << 8) | lo);
            }
            mono[f] = (short) (sum / channels);
        }
        return mono;
    }

    /** Linear-interpolation resampler — adequate quality for a music box, no extra dependency. */
    private static short[] resampleLinear(short[] in, float fromRate, int toRate) {
        if (in.length == 0 || (int) fromRate == toRate) {
            return in;
        }
        double ratio = toRate / (double) fromRate;
        int outLength = (int) Math.floor(in.length * ratio);
        short[] out = new short[outLength];
        for (int i = 0; i < outLength; i++) {
            double srcPos = i / ratio;
            int idx = (int) srcPos;
            double frac = srcPos - idx;
            short a = in[idx];
            short b = (idx + 1 < in.length) ? in[idx + 1] : a;
            out[i] = (short) Math.round(a + (b - a) * frac);
        }
        return out;
    }

    private static byte[] toLittleEndian(short[] samples) {
        byte[] bytes = new byte[samples.length * 2];
        for (int i = 0; i < samples.length; i++) {
            bytes[i * 2] = (byte) (samples[i] & 0xFF);
            bytes[i * 2 + 1] = (byte) ((samples[i] >> 8) & 0xFF);
        }
        return bytes;
    }

    static String extensionOf(Path source) {
        String name = source.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
