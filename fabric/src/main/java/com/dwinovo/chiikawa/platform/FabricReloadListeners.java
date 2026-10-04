package com.dwinovo.chiikawa.platform;

import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;

/** Fabric only takes reload listeners under a name of their own; this names the mod's. */
public final class FabricReloadListeners {
    private FabricReloadListeners() {
    }

    /**
     * @param type server data or client resources
     * @param id the listener's name
     * @param listener the vanilla listener to run under it
     */
    public static void register(PackType type, Identifier id, PreparableReloadListener listener) {
        ResourceLoader.get(type).registerReloader(id, listener);
    }

    /**
     * Fabric runs listeners in no order of its own unless told one.
     *
     * @param type server data or client resources
     * @param first the listener that has to have run
     * @param second the listener that needs what the first loaded
     */
    public static void runBefore(PackType type, Identifier first, Identifier second) {
        ResourceLoader.get(type).addReloaderOrdering(first, second);
    }
}
