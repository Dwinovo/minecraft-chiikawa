package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.init.InitSounds;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

public final class SoundData {
    private static final int MAX_VARIANTS = 64;

    private SoundData() {
    }

    public static Map<String, List<Identifier>> collectVariants(PackOutput output) {
        return collectVariants(resolveResourceRoot(output));
    }

    public static Map<String, List<Identifier>> collectVariants(Path resourceRoot) {
        Map<String, List<Identifier>> variants = new HashMap<>();
        for (InitSounds.SoundEntry entry : InitSounds.entries()) {
            variants.put(entry.path(), findVariants(entry.path(), resourceRoot));
        }
        return variants;
    }

    public static List<Identifier> findVariants(String basePath, Path resourceRoot) {
        List<Identifier> sounds = new ArrayList<>();
        for (int i = 1; i <= MAX_VARIANTS; i++) {
            Identifier candidate = Identifier.fromNamespaceAndPath(Constants.MOD_ID, basePath + "_" + i);
            if (!soundExists(candidate, resourceRoot)) {
                break;
            }
            sounds.add(candidate);
        }

        if (sounds.isEmpty()) {
            Identifier direct = Identifier.fromNamespaceAndPath(Constants.MOD_ID, basePath);
            if (soundExists(direct, resourceRoot)) {
                sounds.add(direct);
            }
        }

        return sounds;
    }

    public static boolean soundExists(Identifier sound, Path resourceRoot) {
        Path path = resourceRoot.resolve("assets")
            .resolve(sound.getNamespace())
            .resolve("sounds")
            .resolve(sound.getPath() + ".ogg");
        return Files.exists(path);
    }

    public static Path resolveResourceRoot(PackOutput output) {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        Path current = userDir;
        for (int i = 0; i < 8 && current != null; i++) {
            Path commonCandidate = current.resolve("common").resolve("src").resolve("main").resolve("resources");
            if (Files.exists(commonCandidate)) {
                return commonCandidate;
            }
            current = current.getParent();
        }
        // 如果找不到 common 资源目录，就回到工程根目录的默认路径（仅作为兜底）
        return Path.of(System.getProperty("user.dir"), "common", "src", "main", "resources");
    }
}
