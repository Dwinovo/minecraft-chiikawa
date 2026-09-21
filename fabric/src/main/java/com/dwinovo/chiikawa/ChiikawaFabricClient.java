package com.dwinovo.chiikawa;

import com.dwinovo.chiikawa.anim.compile.BedrockResourceLoader;
import com.dwinovo.chiikawa.anim.render.PropRenderer;
import com.dwinovo.chiikawa.anim.render.impl.ChiikawaRenderer;
import com.dwinovo.chiikawa.anim.render.impl.FuruhonyaRenderer;
import com.dwinovo.chiikawa.anim.render.impl.HachiwareRenderer;
import com.dwinovo.chiikawa.anim.render.impl.KurimanjuRenderer;
import com.dwinovo.chiikawa.anim.render.impl.MomongaRenderer;
import com.dwinovo.chiikawa.anim.render.impl.RakkoRenderer;
import com.dwinovo.chiikawa.anim.render.impl.ShisaRenderer;
import com.dwinovo.chiikawa.anim.render.impl.UsagiRenderer;
import com.dwinovo.chiikawa.client.music.ClientMusicStreamManager;
import com.dwinovo.chiikawa.client.render.LaborBoardRenderer;
import com.dwinovo.chiikawa.client.render.PropBlockRenderer;
import com.dwinovo.chiikawa.client.screen.PetBackpackScreen;
import com.dwinovo.chiikawa.init.InitBlockEntities;
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.init.InitMenu;
import com.dwinovo.chiikawa.platform.FabricModNetworking;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

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
        for (Supplier<? extends Item> prop : InitItems.PROPS) {
            BuiltinItemRendererRegistry.INSTANCE.register(prop.get(), PropRenderer::drawItem);
        }
        BlockEntityRenderers.register(InitBlockEntities.LABOR_BOARD.get(), context -> new LaborBoardRenderer());
        BlockEntityRenderers.register(InitBlockEntities.SHOP.get(), context -> new PropBlockRenderer<>());

        MenuScreens.register(InitMenu.PET_BACKPACK.get(), PetBackpackScreen::new);
        FabricModNetworking.registerClient();
        ClientTickEvents.END_CLIENT_TICK.register(client -> ClientMusicStreamManager.tick());

        BedrockResourceLoader loader = new BedrockResourceLoader();
        ResourceLocation loaderId = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "anim_loader");
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES)
                .registerReloadListener(new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public ResourceLocation getFabricId() {
                        return loaderId;
                    }

                    @Override
                    public void onResourceManagerReload(ResourceManager rm) {
                        loader.onResourceManagerReload(rm);
                    }
                });
    }
}
