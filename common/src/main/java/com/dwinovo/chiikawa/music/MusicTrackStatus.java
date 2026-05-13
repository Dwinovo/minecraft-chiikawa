package com.dwinovo.chiikawa.music;

import com.mojang.serialization.Codec;

public enum MusicTrackStatus {
    READY,
    IMPORTING,
    FAILED;

    public static final Codec<MusicTrackStatus> CODEC = Codec.STRING.xmap(
        name -> {
            try {
                return MusicTrackStatus.valueOf(name.toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                return FAILED;
            }
        },
        status -> status.name().toLowerCase(java.util.Locale.ROOT)
    );
}
