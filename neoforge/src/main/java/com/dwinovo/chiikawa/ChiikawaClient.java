package com.dwinovo.chiikawa;

import net.minecraft.client.renderer.entity.EntityRenderers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
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

// Client-only mod entry.
@Mod(value = Chiikawa.MODID, dist = Dist.CLIENT)
// Auto-register @SubscribeEvent methods.
@EventBusSubscriber(modid = Chiikawa.MODID, value = Dist.CLIENT)
public class ChiikawaClient {
    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Phase 1: only chiikawa swaps onto the new Bedrock pipeline; the rest stay
            // on the GeckoLib-based renderer until phase 4. Mirrors the Fabric setup.
            EntityRenderers.register(InitEntity.USAGI_PET.get(), UsagiRender::new);
            EntityRenderers.register(InitEntity.HACHIWARE_PET.get(), HachiwareRender::new);
            EntityRenderers.register(InitEntity.CHIIKAWA_PET.get(), ChiikawaRenderer::new);
            EntityRenderers.register(InitEntity.SHISA_PET.get(), ShisaRender::new);
            EntityRenderers.register(InitEntity.MOMONGA_PET.get(), MomongaRender::new);
            EntityRenderers.register(InitEntity.KURIMANJU_PET.get(), KurimanjuRender::new);
            EntityRenderers.register(InitEntity.RAKKO_PET.get(), RakkoRender::new);
        });
    }

    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(InitMenu.PET_BACKPACK.get(), PetBackpackScreen::new);
    }
}


