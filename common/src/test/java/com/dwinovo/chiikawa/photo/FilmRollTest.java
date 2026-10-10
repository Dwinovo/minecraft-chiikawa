package com.dwinovo.chiikawa.photo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class FilmRollTest {
    private static PhotoData shot(int day) {
        return new PhotoData(UUID.randomUUID(), PhotoSubject.FRIEND, "Chiikawa", day, "Hachiware");
    }

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void freshFilmIsNotARollAndIsNeverFull() {
        assertFalse(FilmRoll.NONE.started());
        assertFalse(FilmRoll.NONE.full());
    }

    @Test
    void aRollFillsShotByShotAndIsFullAtItsExposures() {
        FilmRoll roll = FilmRoll.begun(3, shot(1));

        assertTrue(roll.started());
        assertFalse(roll.full());
        roll = roll.with(shot(2));
        assertFalse(roll.full());
        roll = roll.with(shot(3));
        assertTrue(roll.full());
        assertEquals(3, roll.shots().size());
    }

    @Test
    void addingAShotLeavesTheRollItWasAddedToAlone() {
        FilmRoll one = FilmRoll.begun(8, shot(1));

        one.with(shot(2));

        assertEquals(1, one.shots().size());
    }

    @Test
    void shotsKeepTheirOrder() {
        PhotoData first = shot(1);
        PhotoData second = shot(2);

        assertEquals(List.of(first, second), FilmRoll.begun(8, first).with(second).shots());
    }

    @Test
    void whatIsSavedIsWhatIsRead() {
        FilmRoll roll = FilmRoll.begun(8, shot(1)).with(shot(2));

        JsonElement json = FilmRoll.CODEC.encodeStart(JsonOps.INSTANCE, roll).getOrThrow();

        assertEquals(roll, FilmRoll.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    @Test
    void whatIsSentIsWhatIsRead() {
        FilmRoll roll = FilmRoll.begun(8, shot(1)).with(shot(2));
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);

        FilmRoll.STREAM_CODEC.encode(buffer, roll);

        assertEquals(roll, FilmRoll.STREAM_CODEC.decode(buffer));
    }

    @Test
    void aRollOfNoExposuresOrTooManyIsNotRead() {
        String shots = "\"shots\": []";
        assertTrue(FilmRoll.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{ \"exposures\": 0, " + shots + " }")).isError());
        assertTrue(FilmRoll.CODEC.parse(JsonOps.INSTANCE,
            JsonParser.parseString("{ \"exposures\": " + (FilmRoll.MOST_EXPOSURES + 1) + ", " + shots + " }")).isError());
    }
}
