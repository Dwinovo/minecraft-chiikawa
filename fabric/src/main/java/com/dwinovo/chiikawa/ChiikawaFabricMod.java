package com.dwinovo.chiikawa;

import net.fabricmc.api.ModInitializer;
import com.dwinovo.chiikawa.command.ChiikawaDebugCommand;
import com.dwinovo.chiikawa.command.ChiikawaMusicCommand;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.init.InitRegistry;
import com.dwinovo.chiikawa.init.InitSensor;
import com.dwinovo.chiikawa.init.InitTag;
import com.dwinovo.chiikawa.init.InitActivity;
import com.dwinovo.chiikawa.init.InitSounds;
import com.dwinovo.chiikawa.init.InitMenu;
import com.dwinovo.chiikawa.init.InitDataComponents;
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.init.InitBlockEntities;
import com.dwinovo.chiikawa.init.InitBlocks;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.init.InitTabs;
import com.dwinovo.chiikawa.init.InitCapabilities;
import com.dwinovo.chiikawa.entity.PetFollowKeeper;
import com.dwinovo.chiikawa.entity.PetRecall;
import com.dwinovo.chiikawa.entity.brain.personality.PetPersonalityLoader;
import com.dwinovo.chiikawa.platform.FabricReloadListeners;
import com.dwinovo.chiikawa.shop.ShopCatalogLoader;
import com.dwinovo.chiikawa.social.PetInteractionLoader;
import com.dwinovo.chiikawa.spawn.FabricPetSpawns;
import com.dwinovo.chiikawa.spawn.PetSpawnLoader;
import com.dwinovo.chiikawa.task.BoardLevelsLoader;
import com.dwinovo.chiikawa.task.PetTaskTypeLoader;
import com.dwinovo.chiikawa.voice.PetVoiceLoader;
import com.dwinovo.chiikawa.entity.brain.task.farmer.crop.FarmRegistry;
import com.dwinovo.chiikawa.item.PetDollItem;
import com.dwinovo.chiikawa.item.PetReviveRitualManager;
import com.dwinovo.chiikawa.music.ServerMusicSystem;
import com.dwinovo.chiikawa.platform.FabricModNetworking;
import com.dwinovo.chiikawa.platform.Services;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;

public class ChiikawaFabricMod implements ModInitializer {
    @Override
    public void onInitialize() {
        InitRegistry.init();
        InitMemory.init();
        InitSensor.init();
        InitTag.init();
        InitActivity.init();
        InitSounds.init();
        InitMenu.init();
        InitDataComponents.init();
        InitEntity.init();
        InitBlocks.init();
        InitBlockEntities.init();
        InitItems.init();
        InitTabs.init();
        InitCapabilities.register(null);
        FarmRegistry.init();
        FabricPetSpawns.init();
        Services.ENTITY.registerAttributes();
        Services.ENTITY.registerSpawnPlacements();
        FabricModNetworking.registerServer();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ChiikawaMusicCommand.register(dispatcher);
            ChiikawaDebugCommand.register(dispatcher);
        });
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof PetDollItem dollItem)) {
                return InteractionResult.PASS;
            }
            return dollItem.tryStartCakeRitual(level, player, stack, hitResult.getBlockPos());
        });
        FabricReloadListeners.register(PackType.SERVER_DATA, PetPersonalityLoader.ID, new PetPersonalityLoader());
        FabricReloadListeners.register(PackType.SERVER_DATA, PetTaskTypeLoader.ID, new PetTaskTypeLoader());
        FabricReloadListeners.register(PackType.SERVER_DATA, ShopCatalogLoader.ID, new ShopCatalogLoader());
        FabricReloadListeners.register(PackType.SERVER_DATA, BoardLevelsLoader.ID, new BoardLevelsLoader());
        FabricReloadListeners.register(PackType.SERVER_DATA, PetSpawnLoader.ID, new PetSpawnLoader());
        FabricReloadListeners.register(PackType.SERVER_DATA, PetVoiceLoader.ID, new PetVoiceLoader());
        FabricReloadListeners.register(PackType.SERVER_DATA, PetInteractionLoader.ID, new PetInteractionLoader());
        ServerTickEvents.END_SERVER_TICK.register(PetReviveRitualManager::tickServer);
        ServerTickEvents.END_SERVER_TICK.register(ServerMusicSystem::tickServer);
        ServerTickEvents.END_SERVER_TICK.register(PetFollowKeeper::tickServer);
        ServerTickEvents.END_SERVER_TICK.register(PetRecall::tickServer);
        ServerLifecycleEvents.SERVER_STOPPED.register(ServerMusicSystem::stopServer);
        Constants.LOG.info("Hello Chiikawa Fabric world!");
        CommonClass.init();
    }
}
