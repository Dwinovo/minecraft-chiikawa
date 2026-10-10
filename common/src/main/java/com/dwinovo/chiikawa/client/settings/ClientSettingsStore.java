package com.dwinovo.chiikawa.client.settings;

import java.nio.file.Path;
import net.minecraft.client.Minecraft;

/**
 * The player's {@link ClientSettings} as the game is using them: read from disk the first
 * time anything asks, and written back whenever the settings screen changes them.
 */
public final class ClientSettingsStore {
    private static volatile ClientSettings current;

    private ClientSettingsStore() {
    }

    public static ClientSettings current() {
        ClientSettings settings = current;
        if (settings == null) {
            settings = ClientSettings.read(file());
            current = settings;
        }
        return settings;
    }

    public static void set(ClientSettings settings) {
        current = settings;
        settings.write(file());
    }

    private static Path file() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config/chiikawa/client.json");
    }
}
