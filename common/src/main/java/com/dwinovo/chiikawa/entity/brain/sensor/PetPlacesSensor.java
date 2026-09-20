package com.dwinovo.chiikawa.entity.brain.sensor;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitBlocks;
import com.dwinovo.chiikawa.init.InitMemory;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.village.poi.PoiManager;

/**
 * Remembers the places a pet's day is built around — the nearest labor board and the
 * nearest shop — found through the level's points of interest, like a villager finds its
 * workstation. Whether the pet may walk that far is up to its intents.
 *
 * <p>One sensor for both because they are looked up the same way and placed about as
 * rarely; two would scan the same points of interest twice a second for no reason.
 */
public class PetPlacesSensor extends Sensor<AbstractPet> {
    private static final int SEARCH_RADIUS = 48;

    public PetPlacesSensor() {
        // Boards and shops are placed rarely; every two seconds is plenty.
        super(40);
    }

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(InitMemory.NEAREST_BOARD.get(), InitMemory.NEAREST_SHOP.get());
    }

    @Override
    protected void doTick(ServerLevel level, AbstractPet pet) {
        remember(level, pet, InitBlocks.LABOR_BOARD_POI, InitMemory.NEAREST_BOARD.get());
        remember(level, pet, InitBlocks.SHOP_POI, InitMemory.NEAREST_SHOP.get());
    }

    private static void remember(ServerLevel level, AbstractPet pet,
                                 ResourceKey<PoiType> poi, MemoryModuleType<BlockPos> memory) {
        Brain<AbstractPet> brain = pet.getBrain();
        level.getPoiManager()
            .findClosest(type -> type.is(poi), pet.blockPosition(), SEARCH_RADIUS, PoiManager.Occupancy.ANY)
            .ifPresentOrElse(
                pos -> brain.setMemory(memory, pos),
                () -> brain.eraseMemory(memory));
    }
}
