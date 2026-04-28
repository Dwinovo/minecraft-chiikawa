package com.dwinovo.chiikawa;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import com.dwinovo.chiikawa.anim.compile.BedrockResourceLoader;
import com.dwinovo.chiikawa.anim.render.impl.ChiikawaRenderer;
import com.dwinovo.chiikawa.anim.render.impl.HachiwareRenderer;
import com.dwinovo.chiikawa.anim.render.impl.KurimanjuRenderer;
import com.dwinovo.chiikawa.anim.render.impl.MomongaRenderer;
import com.dwinovo.chiikawa.anim.render.impl.RakkoRenderer;
import com.dwinovo.chiikawa.anim.render.impl.ShisaRenderer;
import com.dwinovo.chiikawa.anim.render.impl.UsagiRenderer;
import com.dwinovo.chiikawa.client.screen.PetBackpackScreen;
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.init.InitMenu;

@Mod.EventBusSubscriber(modid = ChiikawaForge.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ChiikawaForgeClient {
    
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // All 7 pets on the Bedrock pipeline. Mirrors the Fabric setup.
            EntityRenderers.register(InitEntity.USAGI_PET.get(), UsagiRenderer::new);
            EntityRenderers.register(InitEntity.HACHIWARE_PET.get(), HachiwareRenderer::new);
            EntityRenderers.register(InitEntity.CHIIKAWA_PET.get(), ChiikawaRenderer::new);
            EntityRenderers.register(InitEntity.SHISA_PET.get(), ShisaRenderer::new);
            EntityRenderers.register(InitEntity.MOMONGA_PET.get(), MomongaRenderer::new);
            EntityRenderers.register(InitEntity.KURIMANJU_PET.get(), KurimanjuRenderer::new);
            EntityRenderers.register(InitEntity.RAKKO_PET.get(), RakkoRenderer::new);

            MenuScreens.register(InitMenu.PET_BACKPACK.get(), PetBackpackScreen::new);
        });
    }

    @SubscribeEvent
    static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new BedrockResourceLoader());
    }
}
