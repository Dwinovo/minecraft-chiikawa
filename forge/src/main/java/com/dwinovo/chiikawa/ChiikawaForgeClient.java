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
import com.dwinovo.chiikawa.client.screen.PetBackpackScreen;
import com.dwinovo.chiikawa.init.InitBlockEntities;
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.init.InitMenu;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = ChiikawaForge.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ChiikawaForgeClient {
    /**
     * Props are drawn from their own Bedrock models, as items as everywhere else. Forge
     * 1.20.1 has no event to hand an item its client extensions; each prop takes these from
     * its own {@code initializeClient} (see {@code ForgeRegistryHelper}).
     */
    public static final IClientItemExtensions PROP_ITEM_EXTENSIONS = new IClientItemExtensions() {
        private BlockEntityWithoutLevelRenderer renderer;

        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            if (renderer == null) {
                renderer = new PropItemRenderer();
            }
            return renderer;
        }
    };

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            EntityRenderers.register(InitEntity.USAGI_PET.get(), UsagiRenderer::new);
            EntityRenderers.register(InitEntity.HACHIWARE_PET.get(), HachiwareRenderer::new);
            EntityRenderers.register(InitEntity.CHIIKAWA_PET.get(), ChiikawaRenderer::new);
            EntityRenderers.register(InitEntity.SHISA_PET.get(), ShisaRenderer::new);
            EntityRenderers.register(InitEntity.MOMONGA_PET.get(), MomongaRenderer::new);
            EntityRenderers.register(InitEntity.KURIMANJU_PET.get(), KurimanjuRenderer::new);
            EntityRenderers.register(InitEntity.RAKKO_PET.get(), RakkoRenderer::new);
            EntityRenderers.register(InitEntity.FURUHONYA_PET.get(), FuruhonyaRenderer::new);
            MenuScreens.register(InitMenu.PET_BACKPACK.get(), PetBackpackScreen::new);
        });
        MinecraftForge.EVENT_BUS.addListener(ChiikawaForgeClient::onClientTick);
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ClientMusicStreamManager.tick();
        }
    }

    /** The built-in item renderer Forge wants, handing each prop to {@link PropRenderer}. */
    private static final class PropItemRenderer extends BlockEntityWithoutLevelRenderer {
        private PropItemRenderer() {
            super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                MultiBufferSource buffers, int light, int overlay) {
            PropRenderer.drawItem(stack, context, pose, buffers, light, overlay);
        }
    }

    @SubscribeEvent
    static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(InitBlockEntities.LABOR_BOARD.get(), context -> new LaborBoardRenderer());
    }

    @SubscribeEvent
    static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(InitBlockEntities.LABOR_BOARD.get(), context -> new LaborBoardRenderer());
    }

    @SubscribeEvent
    static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new BedrockResourceLoader());
    }
}
