package com.dwinovo.chiikawa;

import net.minecraft.client.renderer.entity.EntityRenderers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
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

// Client-only mod entry.
@Mod(value = Chiikawa.MODID, dist = Dist.CLIENT)
// Auto-register @SubscribeEvent methods.
@EventBusSubscriber(modid = Chiikawa.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
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
        });
    }

    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(InitMenu.PET_BACKPACK.get(), PetBackpackScreen::new);
    }
}


