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
