package com.dwinovo.chiikawa.music;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

class MusicBoxSelectionTest {
    @Test
    void codecRoundTripsSelection() {
        MusicBoxSelection selection = new MusicBoxSelection("guitar-1234abcd", "guitar", 7);

        MusicBoxSelection decoded = MusicBoxSelection.CODEC.parse(
            JsonOps.INSTANCE,
            MusicBoxSelection.CODEC.encodeStart(JsonOps.INSTANCE, selection).getOrThrow()
        ).getOrThrow();

        assertEquals(selection, decoded);
        assertEquals("guitar-1234abcd:7", decoded.signature());
    }

    @Test
    void streamCodecRoundTripsSelection() {
        MusicBoxSelection selection = new MusicBoxSelection("track", "Song", 3);
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);

        MusicBoxSelection.STREAM_CODEC.encode(buffer, selection);

        assertEquals(selection, MusicBoxSelection.STREAM_CODEC.decode(buffer));
    }
}
