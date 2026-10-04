package com.dwinovo.chiikawa.platform;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

/** Fabric only takes reload listeners that name themselves; this names the mod's. */
public final class FabricReloadListeners {
    private FabricReloadListeners() {
    }

    /**
     * @param type server data or client resources
     * @param id the listener's name
     * @param listener the vanilla listener to run under it
     * @param after the listeners that have to have run, which Fabric runs in no order of its own unless told one
     */
    public static void register(PackType type, ResourceLocation id, PreparableReloadListener listener,
            ResourceLocation... after) {
        ResourceManagerHelper.get(type).registerReloadListener(new IdentifiableResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return id;
            }

            @Override
            public Collection<ResourceLocation> getFabricDependencies() {
                return List.of(after);
            }

            @Override
            public CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier barrier, ResourceManager manager,
                    ProfilerFiller prepareProfiler, ProfilerFiller applyProfiler, Executor backgroundExecutor, Executor gameExecutor) {
                return listener.reload(barrier, manager, prepareProfiler, applyProfiler, backgroundExecutor, gameExecutor);
            }
        });
    }
}
