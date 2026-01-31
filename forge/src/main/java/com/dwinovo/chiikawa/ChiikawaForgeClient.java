package com.dwinovo.chiikawa;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import com.dwinovo.chiikawa.client.render.impl.ChiikawaRender;
import com.dwinovo.chiikawa.client.render.impl.HachiwareRender;
import com.dwinovo.chiikawa.client.render.impl.KurimanjuRender;
import com.dwinovo.chiikawa.client.render.impl.MomongaRender;
import com.dwinovo.chiikawa.client.render.impl.RakkoRender;
import com.dwinovo.chiikawa.client.render.impl.ShisaRender;
import com.dwinovo.chiikawa.client.render.impl.UsagiRender;
import com.dwinovo.chiikawa.client.screen.PetBackpackScreen;
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.init.InitMenu;

@Mod.EventBusSubscriber(modid = ChiikawaForge.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ChiikawaForgeClient {
    
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            EntityRenderers.register(InitEntity.USAGI_PET.get(), UsagiRender::new);
            EntityRenderers.register(InitEntity.HACHIWARE_PET.get(), HachiwareRender::new);
            EntityRenderers.register(InitEntity.CHIIKAWA_PET.get(), ChiikawaRender::new);
            EntityRenderers.register(InitEntity.SHISA_PET.get(), ShisaRender::new);
            EntityRenderers.register(InitEntity.MOMONGA_PET.get(), MomongaRender::new);
            EntityRenderers.register(InitEntity.KURIMANJU_PET.get(), KurimanjuRender::new);
            EntityRenderers.register(InitEntity.RAKKO_PET.get(), RakkoRender::new);
            
            MenuScreens.register(InitMenu.PET_BACKPACK.get(), PetBackpackScreen::new);
        });
    }
}
