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
import com.dwinovo.chiikawa.entity.brain.personality.PetPersonalityLoader;
import com.dwinovo.chiikawa.task.PetTaskTypeLoader;
import com.dwinovo.chiikawa.entity.brain.task.farmer.crop.FarmRegistry;
import com.dwinovo.chiikawa.item.PetDollItem;
import com.dwinovo.chiikawa.item.PetReviveRitualManager;
import com.dwinovo.chiikawa.data.FabricBiomeModifications;
import com.dwinovo.chiikawa.music.ServerMusicSystem;
import com.dwinovo.chiikawa.platform.FabricMusicNetworking;
import com.dwinovo.chiikawa.platform.Services;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
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
        FarmRegistry.init();
        CreativeModeTabEvents.modifyOutputEvent(InitTabs.MAIN_KEY).register(output -> InitTabs.addMainItems(output::accept));
        FabricBiomeModifications.init();
        Services.ENTITY.registerAttributes();
        Services.ENTITY.registerSpawnPlacements();
        FabricMusicNetworking.registerServer();
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
        registerServerDataListener(PetPersonalityLoader.ID, new PetPersonalityLoader());
        registerServerDataListener(PetTaskTypeLoader.ID, new PetTaskTypeLoader());
        ServerTickEvents.END_SERVER_TICK.register(PetReviveRitualManager::tickServer);
        ServerTickEvents.END_SERVER_TICK.register(ServerMusicSystem::tickServer);
        ServerLifecycleEvents.SERVER_STOPPED.register(ServerMusicSystem::stopServer);
        Constants.LOG.info("Hello Chiikawa Fabric world!");
        CommonClass.init();
    }

    /** Fabric only takes reload listeners that name themselves. */
    private static void registerServerDataListener(ResourceLocation id, PreparableReloadListener listener) {
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new IdentifiableResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return id;
            }

            @Override
            public CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier barrier, ResourceManager manager,
                    ProfilerFiller prepareProfiler, ProfilerFiller applyProfiler, Executor backgroundExecutor, Executor gameExecutor) {
                return listener.reload(barrier, manager, prepareProfiler, applyProfiler, backgroundExecutor, gameExecutor);
            }
        });
    }
}
