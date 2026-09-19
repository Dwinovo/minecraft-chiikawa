package com.dwinovo.chiikawa.entity.brain.sensor;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitBlocks;
import com.dwinovo.chiikawa.init.InitMemory;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.village.poi.PoiManager;

/**
 * Remembers the labor board nearest the pet, found through the level's points of
 * interest like a villager finds its workstation. Whether the pet may walk that far is
 * up to its intents.
 */
public class PetBoardSensor extends Sensor<AbstractPet> {
    private static final int SEARCH_RADIUS = 48;

    public PetBoardSensor() {
        // Boards are placed rarely; every two seconds is plenty.
        super(40);
    }

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(InitMemory.NEAREST_BOARD.get());
    }

    @Override
    protected void doTick(ServerLevel level, AbstractPet pet) {
        Brain<AbstractPet> brain = pet.getBrain();
        level.getPoiManager()
            .findClosest(type -> type.is(InitBlocks.LABOR_BOARD_POI), pet.blockPosition(), SEARCH_RADIUS, PoiManager.Occupancy.ANY)
            .ifPresentOrElse(
                pos -> brain.setMemory(InitMemory.NEAREST_BOARD.get(), pos),
                () -> brain.eraseMemory(InitMemory.NEAREST_BOARD.get()));
    }
}
