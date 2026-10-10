package com.dwinovo.chiikawa.client.settings;

import com.dwinovo.chiikawa.settings.WorldSettings;

/**
 * The world's {@link WorldSettings} as the server last told this game, and whether this
 * player may change them. The server sends them on joining and whenever they change, so
 * what is here is stale outside a world and is only ever read inside one.
 */
public final class ClientWorldSettings {
    /**
     * @param settings what the server said
     * @param canChange whether the server will take a change from this player
     */
    public record Synced(WorldSettings settings, boolean canChange) {
        /** What there is before the server has said anything. */
        public static final Synced NONE = new Synced(WorldSettings.DEFAULT, false);
    }

    private static volatile Synced current = Synced.NONE;

    private ClientWorldSettings() {
    }

    public static Synced current() {
        return current;
    }

    public static void accept(Synced synced) {
        current = synced;
    }
}
