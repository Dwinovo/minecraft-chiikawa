package com.dwinovo.chiikawa;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.init.InitMenu;
import com.dwinovo.chiikawa.init.InitSensor;
import com.dwinovo.chiikawa.init.InitActivity;
import com.dwinovo.chiikawa.init.InitCapabilities;
import com.dwinovo.chiikawa.init.InitSounds;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.init.InitTabs;
import com.dwinovo.chiikawa.platform.ForgePlatformRegistryAccess;
import com.dwinovo.chiikawa.platform.Services;

@Mod(ChiikawaForge.MODID)
public class ChiikawaForge {
    
    public static final String MODID = "chiikawa";

    public ChiikawaForge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register Forge platform registries first (includes custom PetJob registry)
        ForgePlatformRegistryAccess.register(modEventBus);

        // Initialize other registries
        com.dwinovo.chiikawa.init.InitRegistry.init();
        InitMemory.init();
        InitSensor.init();
        InitActivity.init();
        InitSounds.init();
        InitMenu.init();
        InitEntity.init();
        InitItems.init();
        InitTabs.init();
        InitCapabilities.init();
        Services.REGISTRY.registerToEventBus(modEventBus);
        Services.ENTITY.registerToEventBus(modEventBus);

        InitCapabilities.register(modEventBus);

        Constants.LOG.info("Hello Chiikawa Forge world!");
        CommonClass.init();
    }
}

