package com.dwinovo.chiikawa.music;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class CmsTrackFile {
    private static final int MAGIC = 0x434D5331; // CMS1
    private static final int VERSION = 1;

    private CmsTrackFile() {
    }

    public static void write(Path path, CachedOpusTrack track) throws IOException {
        Files.createDirectories(path.getParent());
        try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(path)))) {
            output.writeInt(MAGIC);
            output.writeInt(VERSION);
            output.writeInt(track.sampleRate());
            output.writeInt(track.channels());
            output.writeInt(track.frameSamples());
            output.writeInt(track.frameCount());
            for (byte[] frame : track.frames()) {
                output.writeShort(frame.length);
                output.write(frame);
            }
        }
    }

    public static CachedOpusTrack read(Path path) throws IOException {
        try (DataInputStream input = new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
            int magic = input.readInt();
            int version = input.readInt();
            if (magic != MAGIC || version != VERSION) {
                throw new IOException("Unsupported CMS track file: " + path);
            }

            int sampleRate = input.readInt();
            int channels = input.readInt();
            int frameSamples = input.readInt();
            int frameCount = input.readInt();
            List<byte[]> frames = new ArrayList<>(frameCount);
            for (int i = 0; i < frameCount; i++) {
                int length = input.readUnsignedShort();
                byte[] frame = input.readNBytes(length);
                if (frame.length != length) {
                    throw new IOException("Truncated CMS frame " + i + " in " + path);
                }
                frames.add(frame);
            }
            return new CachedOpusTrack(sampleRate, channels, frameSamples, List.copyOf(frames));
        }
    }
}
