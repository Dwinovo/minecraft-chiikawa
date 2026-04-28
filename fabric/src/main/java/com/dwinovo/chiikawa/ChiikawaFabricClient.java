package com.dwinovo.chiikawa;

import com.dwinovo.chiikawa.anim.compile.BedrockResourceLoader;
import com.dwinovo.chiikawa.anim.render.impl.ChiikawaRenderer;
import com.dwinovo.chiikawa.client.render.impl.HachiwareRender;
import com.dwinovo.chiikawa.client.render.impl.KurimanjuRender;
import com.dwinovo.chiikawa.client.render.impl.MomongaRender;
import com.dwinovo.chiikawa.client.render.impl.RakkoRender;
import com.dwinovo.chiikawa.client.render.impl.ShisaRender;
import com.dwinovo.chiikawa.client.render.impl.UsagiRender;
import com.dwinovo.chiikawa.client.screen.PetBackpackScreen;
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.init.InitMenu;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;

public class ChiikawaFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Phase 1: only chiikawa swaps onto the new Bedrock pipeline; the rest stay on
        // the GeckoLib-based renderer until phase 4. This lets us validate phase-by-phase.
        EntityRendererRegistry.register(InitEntity.USAGI_PET.get(), UsagiRender::new);
        EntityRendererRegistry.register(InitEntity.HACHIWARE_PET.get(), HachiwareRender::new);
        EntityRendererRegistry.register(InitEntity.CHIIKAWA_PET.get(), ChiikawaRenderer::new);
        EntityRendererRegistry.register(InitEntity.SHISA_PET.get(), ShisaRender::new);
        EntityRendererRegistry.register(InitEntity.MOMONGA_PET.get(), MomongaRender::new);
        EntityRendererRegistry.register(InitEntity.KURIMANJU_PET.get(), KurimanjuRender::new);
        EntityRendererRegistry.register(InitEntity.RAKKO_PET.get(), RakkoRender::new);

        MenuScreens.register(InitMenu.PET_BACKPACK.get(), PetBackpackScreen::new);

        BedrockResourceLoader loader = new BedrockResourceLoader();
        Identifier loaderId = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "anim_loader");
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES)
                .registerReloadListener(new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public Identifier getFabricId() {
                        return loaderId;
                    }

                    @Override
                    public void onResourceManagerReload(ResourceManager rm) {
                        loader.onResourceManagerReload(rm);
                    }
                });
    }
}
