package com.dwinovo.chiikawa;

import com.dwinovo.chiikawa.command.ChiikawaDebugCommand;
import com.dwinovo.chiikawa.command.ChiikawaMusicCommand;
import com.dwinovo.chiikawa.init.InitActivity;
import com.dwinovo.chiikawa.init.InitCapabilities;
import com.dwinovo.chiikawa.init.InitDataComponents;
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.init.InitBlockEntities;
import com.dwinovo.chiikawa.init.InitBlocks;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.init.InitMenu;
import com.dwinovo.chiikawa.init.InitRegistry;
import com.dwinovo.chiikawa.init.InitSensor;
import com.dwinovo.chiikawa.init.InitSounds;
import com.dwinovo.chiikawa.init.InitTabs;
import com.dwinovo.chiikawa.entity.PetFollowKeeper;
import com.dwinovo.chiikawa.entity.PetRecall;
import com.dwinovo.chiikawa.entity.brain.personality.PetPersonalityLoader;
import com.dwinovo.chiikawa.shop.ShopCatalogLoader;
import com.dwinovo.chiikawa.spawn.PetSpawnLoader;
import com.dwinovo.chiikawa.spawn.PetSpawnsBiomeModifier;
import com.dwinovo.chiikawa.task.BoardLevelsLoader;
import com.dwinovo.chiikawa.task.PetTaskTypeLoader;
import com.dwinovo.chiikawa.voice.PetVoiceLoader;
import com.dwinovo.chiikawa.entity.brain.task.farmer.crop.FarmRegistry;
import com.dwinovo.chiikawa.item.PetDollItem;
import com.dwinovo.chiikawa.item.PetReviveRitualManager;
import com.dwinovo.chiikawa.music.ServerMusicSystem;
import com.dwinovo.chiikawa.platform.ForgeModNetworking;
import com.dwinovo.chiikawa.platform.ForgePlatformRegistryAccess;
import com.dwinovo.chiikawa.platform.Services;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ChiikawaForge.MODID)
public class ChiikawaForge {
    public static final String MODID = "chiikawa";

    public ChiikawaForge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ForgePlatformRegistryAccess.register(modEventBus);

        InitRegistry.init();
        InitMemory.init();
        InitSensor.init();
        InitActivity.init();
        InitSounds.init();
        InitMenu.init();
        InitDataComponents.init();
        InitEntity.init();
        InitBlocks.init();
        InitBlockEntities.init();
        InitItems.init();
        InitTabs.init();
        InitCapabilities.init();
        FarmRegistry.init();
        Services.REGISTRY.registerToEventBus(modEventBus);
        Services.ENTITY.registerToEventBus(modEventBus);
        PetSpawnsBiomeModifier.register(modEventBus);
        ForgeModNetworking.register();

        MinecraftForge.EVENT_BUS.addListener(ChiikawaForge::onRightClickBlock);
        MinecraftForge.EVENT_BUS.addListener(ChiikawaForge::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(ChiikawaForge::onServerStopping);
        MinecraftForge.EVENT_BUS.addListener(ChiikawaForge::registerCommands);
        MinecraftForge.EVENT_BUS.addListener((AddReloadListenerEvent event) -> {
            event.addListener(new PetPersonalityLoader());
            event.addListener(new PetTaskTypeLoader());
            event.addListener(new ShopCatalogLoader());
            event.addListener(new BoardLevelsLoader());
            event.addListener(new PetSpawnLoader());
            event.addListener(new PetVoiceLoader());
        });

        InitCapabilities.register(modEventBus);

        Constants.LOG.info("Hello Chiikawa Forge world!");
        CommonClass.init();
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof PetDollItem dollItem)) {
            return;
        }

        InteractionResult result = dollItem.tryStartCakeRitual(
            event.getLevel(),
            event.getEntity(),
            stack,
            event.getPos()
        );
        if (result != InteractionResult.PASS) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }

    private static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            PetReviveRitualManager.tickServer(event.getServer());
            ServerMusicSystem.tickServer(event.getServer());
            PetFollowKeeper.tickServer(event.getServer());
            PetRecall.tickServer(event.getServer());
        }
    }

    private static void onServerStopping(ServerStoppingEvent event) {
        ServerMusicSystem.stopServer(event.getServer());
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        ChiikawaMusicCommand.register(event.getDispatcher());
        ChiikawaDebugCommand.register(event.getDispatcher());
    }
}
