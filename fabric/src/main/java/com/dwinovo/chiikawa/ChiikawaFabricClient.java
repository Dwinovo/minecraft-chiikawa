package com.dwinovo.chiikawa;

import com.dwinovo.chiikawa.anim.compile.BedrockResourceLoader;
import com.dwinovo.chiikawa.anim.render.impl.ChiikawaRenderer;
import com.dwinovo.chiikawa.anim.render.impl.FuruhonyaRenderer;
import com.dwinovo.chiikawa.anim.render.impl.HachiwareRenderer;
import com.dwinovo.chiikawa.anim.render.impl.KurimanjuRenderer;
import com.dwinovo.chiikawa.anim.render.impl.MomongaRenderer;
import com.dwinovo.chiikawa.anim.render.impl.RakkoRenderer;
import com.dwinovo.chiikawa.anim.render.impl.ShisaRenderer;
import com.dwinovo.chiikawa.anim.render.impl.UsagiRenderer;
import com.dwinovo.chiikawa.client.manual.ManualStageRenderer;
import com.dwinovo.chiikawa.client.music.ClientMusicStreamManager;
import com.dwinovo.chiikawa.client.render.LaborBoardRenderer;
import com.dwinovo.chiikawa.client.render.PropBlockRenderer;
import com.dwinovo.chiikawa.client.render.PropItemRenderer;
import com.dwinovo.chiikawa.client.screen.PetBackpackScreen;
import com.dwinovo.chiikawa.init.InitBlockEntities;
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.init.InitMenu;
import com.dwinovo.chiikawa.manual.ManualLoader;
import com.dwinovo.chiikawa.platform.FabricModNetworking;
import com.dwinovo.chiikawa.platform.FabricReloadListeners;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.SpecialGuiElementRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

public class ChiikawaFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // All 7 pets on the Bedrock pipeline.
        EntityRendererRegistry.register(InitEntity.USAGI_PET.get(), UsagiRenderer::new);
        EntityRendererRegistry.register(InitEntity.HACHIWARE_PET.get(), HachiwareRenderer::new);
        EntityRendererRegistry.register(InitEntity.CHIIKAWA_PET.get(), ChiikawaRenderer::new);
        EntityRendererRegistry.register(InitEntity.SHISA_PET.get(), ShisaRenderer::new);
        EntityRendererRegistry.register(InitEntity.MOMONGA_PET.get(), MomongaRenderer::new);
        EntityRendererRegistry.register(InitEntity.KURIMANJU_PET.get(), KurimanjuRenderer::new);
        EntityRendererRegistry.register(InitEntity.RAKKO_PET.get(), RakkoRenderer::new);
        EntityRendererRegistry.register(InitEntity.FURUHONYA_PET.get(), FuruhonyaRenderer::new);

        // Props are drawn from their own Bedrock models, as items as everywhere else.
        SpecialModelRenderers.ID_MAPPER.put(PropItemRenderer.ID, PropItemRenderer.Unbaked.MAP_CODEC);
        // The handbook's pets and props, drawn into the picture each page hands the game.
        SpecialGuiElementRegistry.register(context -> new ManualStageRenderer(context.vertexConsumers()));
        BlockEntityRenderers.register(InitBlockEntities.LABOR_BOARD.get(), context -> new LaborBoardRenderer());
        BlockEntityRenderers.register(InitBlockEntities.SHOP.get(), context -> new PropBlockRenderer<>());

        MenuScreens.register(InitMenu.PET_BACKPACK.get(), PetBackpackScreen::new);
        FabricModNetworking.registerClient();
        ClientTickEvents.END_CLIENT_TICK.register(client -> ClientMusicStreamManager.tick());

        FabricReloadListeners.register(PackType.CLIENT_RESOURCES,
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "anim_loader"), new BedrockResourceLoader());
        FabricReloadListeners.register(PackType.CLIENT_RESOURCES, ManualLoader.ID, new ManualLoader());
    }
}
