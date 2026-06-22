package com.dwinovo.chiikawa;

import net.fabricmc.api.ModInitializer;
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
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.init.InitTabs;
import com.dwinovo.chiikawa.entity.brain.task.farmer.crop.FarmRegistry;
import com.dwinovo.chiikawa.item.PetDollItem;
import com.dwinovo.chiikawa.item.PetReviveRitualManager;
import com.dwinovo.chiikawa.data.FabricBiomeModifications;
import com.dwinovo.chiikawa.music.ServerMusicSystem;
import com.dwinovo.chiikawa.platform.FabricMusicNetworking;
import com.dwinovo.chiikawa.platform.Services;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
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
        InitItems.init();
        InitTabs.init();
        FarmRegistry.init();
        FabricBiomeModifications.init();
        Services.ENTITY.registerAttributes();
        Services.ENTITY.registerSpawnPlacements();
        FabricMusicNetworking.registerServer();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            ChiikawaMusicCommand.register(dispatcher));
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof PetDollItem dollItem)) {
                return InteractionResult.PASS;
            }
            return dollItem.tryStartCakeRitual(level, player, stack, hitResult.getBlockPos());
        });
        ServerTickEvents.END_SERVER_TICK.register(PetReviveRitualManager::tickServer);
        ServerTickEvents.END_SERVER_TICK.register(ServerMusicSystem::tickServer);
        ServerLifecycleEvents.SERVER_STOPPED.register(ServerMusicSystem::stopServer);
        Constants.LOG.info("Hello Chiikawa Fabric world!");
        CommonClass.init();
    }
}
