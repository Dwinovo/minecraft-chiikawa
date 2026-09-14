package com.dwinovo.chiikawa.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.Constants;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SoundDataTest {
    @TempDir
    Path tempDir;

    @Test
    void numberedVariantsAreCollectedInOrderUntilFirstGap() throws IOException {
        createSound("hachiware/cute_1");
        createSound("hachiware/cute_2");
        createSound("hachiware/cute_4");

        assertEquals(List.of(
                ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "hachiware/cute_1"),
                ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "hachiware/cute_2")
        ), SoundData.findVariants("hachiware/cute", tempDir));
    }

    @Test
    void directSoundIsUsedWhenNoNumberedVariantsExist() throws IOException {
        createSound("hachiware/cute");

        assertEquals(List.of(
                ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "hachiware/cute")
        ), SoundData.findVariants("hachiware/cute", tempDir));
    }

    @Test
    void kurimanjuAndShisaHurtAndDeathClipsResolveInModResources() {
        // Locates common/src/main/resources from the working directory; the PackOutput is unused.
        Path resources = SoundData.resolveResourceRoot(null);
        for (String event : List.of("kurimanju/injured", "kurimanju/death", "shisa/injured", "shisa/death")) {
            assertEquals(List.of(
                    ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, event + "_1")
            ), SoundData.findVariants(event, resources), event);
        }
    }

    @Test
    void missingSoundReturnsEmptyList() {
        assertTrue(SoundData.findVariants("hachiware/missing", tempDir).isEmpty());
    }

    private void createSound(String path) throws IOException {
        Path sound = tempDir.resolve("assets")
                .resolve(Constants.MOD_ID)
                .resolve("sounds")
                .resolve(path + ".ogg");
        Files.createDirectories(sound.getParent());
        Files.createFile(sound);
    }
}
