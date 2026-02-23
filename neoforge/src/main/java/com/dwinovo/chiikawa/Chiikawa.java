package com.dwinovo.chiikawa;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.init.InitMenu;
import com.dwinovo.chiikawa.init.InitRegistry;
import com.dwinovo.chiikawa.init.InitSensor;
import com.dwinovo.chiikawa.init.InitActivity;
import com.dwinovo.chiikawa.init.InitCapabilities;
import com.dwinovo.chiikawa.init.InitSounds;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.init.InitTabs;
import com.dwinovo.chiikawa.item.PetDollItem;
import com.dwinovo.chiikawa.item.PetReviveRitualManager;
import com.dwinovo.chiikawa.platform.NeoForgePlatformRegistryAccess;
import com.dwinovo.chiikawa.platform.Services;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

// Must match the mod id in META-INF/neoforge.mods.toml.
@Mod(Chiikawa.MODID)
public class Chiikawa {
    
    public static final String MODID = "chiikawa";

    public Chiikawa(IEventBus modEventBus) {

        NeoForgePlatformRegistryAccess.register(modEventBus);

        InitRegistry.init();
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
        NeoForge.EVENT_BUS.addListener(Chiikawa::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> PetReviveRitualManager.tickServer(event.getServer()));

        InitCapabilities.register(modEventBus);

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



}
