package com.dwinovo.chiikawa.platform;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;

/** Fabric only takes reload listeners that name themselves; this names the mod's. */
public final class FabricReloadListeners {
    private FabricReloadListeners() {
    }

    /**
     * @param type server data or client resources
     * @param id the listener's name
     * @param listener the vanilla listener to run under it
     */
    public static void register(PackType type, ResourceLocation id, PreparableReloadListener listener) {
        ResourceManagerHelper.get(type).registerReloadListener(new IdentifiableResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return id;
            }

            @Override
            public CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier barrier, ResourceManager manager,
                    Executor backgroundExecutor, Executor gameExecutor) {
                return listener.reload(barrier, manager, backgroundExecutor, gameExecutor);
            }
        });
    }
}
