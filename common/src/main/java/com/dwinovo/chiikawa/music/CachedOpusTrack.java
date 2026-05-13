package com.dwinovo.chiikawa.music;

import java.util.List;

public record CachedOpusTrack(int sampleRate, int channels, int frameSamples, List<byte[]> frames) {
    public int frameCount() {
        return frames.size();
    }

    public int durationTicks() {
        return (int) Math.ceil(frameCount() / 50.0D * 20.0D);
    }

    public List<byte[]> slice(int startFrame, int frameCount) {
        if (startFrame < 0 || startFrame >= frames.size() || frameCount <= 0) {
            return List.of();
        }
        int end = Math.min(frames.size(), startFrame + frameCount);
        return frames.subList(startFrame, end);
    }
}
