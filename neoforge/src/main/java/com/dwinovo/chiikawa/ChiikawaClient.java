package com.dwinovo.chiikawa;

import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import com.dwinovo.chiikawa.anim.compile.BedrockResourceLoader;
import com.dwinovo.chiikawa.anim.render.impl.ChiikawaRenderer;
import com.dwinovo.chiikawa.anim.render.impl.FuruhonyaRenderer;
import com.dwinovo.chiikawa.anim.render.impl.HachiwareRenderer;
import com.dwinovo.chiikawa.anim.render.impl.KurimanjuRenderer;
import com.dwinovo.chiikawa.anim.render.impl.MomongaRenderer;
import com.dwinovo.chiikawa.anim.render.impl.RakkoRenderer;
import com.dwinovo.chiikawa.anim.render.impl.ShisaRenderer;
import com.dwinovo.chiikawa.anim.render.impl.UsagiRenderer;
import com.dwinovo.chiikawa.client.manual.ManualStageRenderState;
import com.dwinovo.chiikawa.client.manual.ManualStageRenderer;
import com.dwinovo.chiikawa.client.music.ClientMusicStreamManager;
import com.dwinovo.chiikawa.client.render.LaborBoardRenderer;
import com.dwinovo.chiikawa.client.render.PropBlockRenderer;
import com.dwinovo.chiikawa.client.render.PropItemRenderer;
import com.dwinovo.chiikawa.client.screen.PetBackpackScreen;
import com.dwinovo.chiikawa.init.InitBlockEntities;
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.init.InitMenu;
import com.dwinovo.chiikawa.platform.NeoForgeModNetworking;
import com.dwinovo.chiikawa.manual.ManualLoader;
import net.neoforged.neoforge.common.NeoForge;

// Client-only mod entry.
@Mod(value = Chiikawa.MODID, dist = Dist.CLIENT)
// Auto-register @SubscribeEvent methods.
@EventBusSubscriber(modid = Chiikawa.MODID, value = Dist.CLIENT)
public class ChiikawaClient {
    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // All 7 pets on the Bedrock pipeline. Mirrors the Fabric setup.
            EntityRenderers.register(InitEntity.USAGI_PET.get(), UsagiRenderer::new);
            EntityRenderers.register(InitEntity.HACHIWARE_PET.get(), HachiwareRenderer::new);
            EntityRenderers.register(InitEntity.CHIIKAWA_PET.get(), ChiikawaRenderer::new);
            EntityRenderers.register(InitEntity.SHISA_PET.get(), ShisaRenderer::new);
            EntityRenderers.register(InitEntity.MOMONGA_PET.get(), MomongaRenderer::new);
            EntityRenderers.register(InitEntity.KURIMANJU_PET.get(), KurimanjuRenderer::new);
            EntityRenderers.register(InitEntity.RAKKO_PET.get(), RakkoRenderer::new);
            EntityRenderers.register(InitEntity.FURUHONYA_PET.get(), FuruhonyaRenderer::new);
            NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post tick) -> ClientMusicStreamManager.tick());
        });
    }

    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(InitMenu.PET_BACKPACK.get(), PetBackpackScreen::new);
    }

    @SubscribeEvent
    static void registerClientPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        NeoForgeModNetworking.registerClientPayloads(event);
    }

    @SubscribeEvent
    static void registerSpecialModels(RegisterSpecialModelRendererEvent event) {
        // Props are drawn from their own Bedrock models, as items as everywhere else.
        event.register(PropItemRenderer.ID, PropItemRenderer.Unbaked.MAP_CODEC);
    }

    @SubscribeEvent
    static void registerPictures(RegisterPictureInPictureRenderersEvent event) {
        // The handbook's pets and props, drawn into the picture each page hands the game.
        event.register(ManualStageRenderState.class, ManualStageRenderer::new);
    }

    @SubscribeEvent
    static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(InitBlockEntities.LABOR_BOARD.get(), context -> new LaborBoardRenderer());
        event.registerBlockEntityRenderer(InitBlockEntities.SHOP.get(), context -> new PropBlockRenderer<>());
    }

    @SubscribeEvent
    static void registerReloadListeners(AddClientReloadListenersEvent event) {
        // Bakes the .geo.json / .animation.json into ModelLibrary + AnimationLibrary.
        // Without this the renderer's ModelLibrary.get(...) returns null and submit()
        // short-circuits — entity exists and is interactable but renders as nothing.
        event.addListener(
                ResourceLocation.fromNamespaceAndPath(Chiikawa.MODID, "anim_loader"),
                new BedrockResourceLoader());
        event.addListener(
                ResourceLocation.fromNamespaceAndPath(Chiikawa.MODID, "manual_loader"),
                new ManualLoader());
    }
}

