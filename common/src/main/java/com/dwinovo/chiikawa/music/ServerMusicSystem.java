package com.dwinovo.chiikawa.music;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.MinecraftServer;

public final class ServerMusicSystem {
    private static final Map<MinecraftServer, ServerMusicSystem> SYSTEMS = new WeakHashMap<>();

    private final ServerMusicLibrary library;
    private final ServerMusicStreamManager streamManager;

    private ServerMusicSystem(MinecraftServer server) {
        this.library = new ServerMusicLibrary(server);
        this.library.init();
        this.streamManager = new ServerMusicStreamManager(library);
    }

    public static ServerMusicLibrary library(MinecraftServer server) {
        return get(server).library;
    }

    public static ServerMusicStreamManager streams(MinecraftServer server) {
        return get(server).streamManager;
    }

    /**
     * For what has no server at hand, such as a rest that waits the music's retry: the settings of the server that
     * is running. A game runs one at a time.
     *
     * @return the music settings, or the defaults while no server has used them yet
     */
    public static ChiikawaMusicConfig config() {
        synchronized (SYSTEMS) {
            return SYSTEMS.values().stream().findFirst().map(system -> system.library.config())
                .orElse(ChiikawaMusicConfig.DEFAULT);
        }
    }

    public static void tickServer(MinecraftServer server) {
        ServerMusicSystem system = get(server);
        system.library.tick();
        system.streamManager.tick();
    }

    public static void stopServer(MinecraftServer server) {
        ServerMusicSystem system = SYSTEMS.remove(server);
        if (system != null) {
            system.library.close();
        }
    }

    private static ServerMusicSystem get(MinecraftServer server) {
        synchronized (SYSTEMS) {
            return SYSTEMS.computeIfAbsent(server, ServerMusicSystem::new);
        }
    }
}
