package com.dwinovo.chiikawa.photo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PhotoDataTest {
    private static final PhotoData FRIEND = new PhotoData(UUID.randomUUID(), PhotoSubject.FRIEND, "Chiikawa", 12,
        "Hachiware");
    private static final PhotoData VIEW = new PhotoData(UUID.randomUUID(), PhotoSubject.SCENERY, "", 3, "Hachiware");

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void whatIsSavedIsWhatIsRead() {
        for (PhotoData photo : new PhotoData[] {FRIEND, VIEW}) {
            JsonElement saved = PhotoData.CODEC.encodeStart(JsonOps.INSTANCE, photo).getOrThrow(false, org.junit.jupiter.api.Assertions::fail);

            assertEquals(photo, PhotoData.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow(false, org.junit.jupiter.api.Assertions::fail));
        }
    }

    @Test
    void whatIsSentIsWhatIsRead() {
        for (PhotoData photo : new PhotoData[] {FRIEND, VIEW}) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

            photo.write(buffer);

            assertEquals(photo, PhotoData.read(buffer));
        }
    }

    @Test
    void everyKindOfSubjectIsSavedAndSent() {
        for (PhotoSubject subject : PhotoSubject.values()) {
            PhotoData photo = new PhotoData(UUID.randomUUID(), subject, "x", 1, "y");
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

            photo.write(buffer);

            assertEquals(subject, PhotoData.read(buffer).subject());
            assertEquals(subject, PhotoData.CODEC.parse(JsonOps.INSTANCE,
                PhotoData.CODEC.encodeStart(JsonOps.INSTANCE, photo).getOrThrow(false, org.junit.jupiter.api.Assertions::fail)).getOrThrow(false, org.junit.jupiter.api.Assertions::fail).subject());
        }
    }

    @Test
    void theCaptionNamesWhoTookItWhenAndWhoIsInIt() {
        TranslatableContents caption = (TranslatableContents) FRIEND.caption().getContents();

        assertEquals("screen.chiikawa.album.caption", caption.getKey());
        assertEquals("Hachiware", caption.getArgs()[0]);
        assertEquals(12L, caption.getArgs()[1]);
        assertEquals("Chiikawa", caption.getArgs()[2]);
    }

    @Test
    void aViewHasNobodyInItToName() {
        TranslatableContents caption = (TranslatableContents) VIEW.caption().getContents();

        assertEquals("screen.chiikawa.album.caption.scenery", caption.getKey());
        assertEquals(2, caption.getArgs().length);
        assertTrue(VIEW.name().isEmpty());
    }
}
