package com.dwinovo.chiikawa.music;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CmsTrackFileTest {
    @TempDir
    Path tempDir;

    @Test
    void writesAndReadsIndexedOpusFrames() throws Exception {
        Path file = tempDir.resolve("track.cms");
        List<byte[]> frames = List.of(new byte[] {1, 2, 3}, new byte[] {4, 5});

        CmsTrackFile.write(file, new CachedOpusTrack(48_000, 1, 960, frames));
        CachedOpusTrack read = CmsTrackFile.read(file);

        assertEquals(48_000, read.sampleRate());
        assertEquals(1, read.channels());
        assertEquals(960, read.frameSamples());
        assertEquals(2, read.frameCount());
        assertArrayEquals(frames.get(0), read.frames().get(0));
        assertArrayEquals(frames.get(1), read.frames().get(1));
    }
}
