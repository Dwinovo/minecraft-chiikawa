package com.dwinovo.chiikawa.entity.brain.sensor;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.view.DayPhase;
import com.dwinovo.chiikawa.init.InitBlocks;
import com.dwinovo.chiikawa.init.InitMemory;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.pathfinder.Path;

/**
 * Remembers the places a pet's day is built around — the nearest labor board, the
 * nearest shop and the nearest bed — found through the level's points of interest, like a villager finds its
 * workstation. Whether the pet may walk that far is up to its errands.
 *
 * <p>One sensor for all three because they are looked up the same way and placed about as
 * rarely; three would scan the same points of interest twice a second for no reason. The bed is the game's own
 * {@link MemoryModuleType#NEAREST_BED}, which the game's {@code NearestBedSensor} writes for a baby villager and for no one
 * else: it is found here the same way, for a pet that is not a baby.
 */
public class PetPlacesSensor extends Sensor<AbstractPet> {
    private static final int SEARCH_RADIUS = 48;
    /** How far the pet looks for a bed: as far as the game's own sensor looks for a villager's. */
    private static final int BED_RADIUS = 16;

    public PetPlacesSensor() {
        // Boards and shops are placed rarely; every two seconds is plenty.
        super(40);
    }

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(InitMemory.NEAREST_BOARD.get(), InitMemory.NEAREST_SHOP.get(), MemoryModuleType.NEAREST_BED);
    }

    @Override
    protected void doTick(ServerLevel level, AbstractPet pet) {
        remember(level, pet, InitBlocks.LABOR_BOARD_POI, InitMemory.NEAREST_BOARD.get());
        remember(level, pet, InitBlocks.SHOP_POI, InitMemory.NEAREST_SHOP.get());
        rememberBed(level, pet);
    }

    /**
     * Remembers the nearest bed the pet could walk to, once the day is ending and for as long as there is a night to sleep in
     * it: a level where a bed works, from dusk, and in a storm, when a player can lie in one. It looks from dusk so that it
     * knows where the bed is by the time it is dark, and lies down on it and not on the floor. By day, and where a bed is no
     * use, it looks for none; and a bed it could not get to, as the game's sensor for a baby villager sees to, is none the pet
     * is sent to.
     */
    private static void rememberBed(ServerLevel level, AbstractPet pet) {
        Brain<AbstractPet> brain = pet.getBrain();
        DayPhase phase = DayPhase.in(level);
        boolean night = level.dimensionType().bedWorks()
            && (phase == DayPhase.EVENING || phase == DayPhase.NIGHT || level.isThundering());
        Optional<BlockPos> bed = night
            ? level.getPoiManager().findClosest(type -> type.is(PoiTypes.HOME), pet.blockPosition(), BED_RADIUS,
                PoiManager.Occupancy.ANY).filter(found -> reachable(pet, found))
            : Optional.empty();
        bed.ifPresentOrElse(found -> brain.setMemory(MemoryModuleType.NEAREST_BED, found),
            () -> brain.eraseMemory(MemoryModuleType.NEAREST_BED));
    }

    private static boolean reachable(AbstractPet pet, BlockPos bed) {
        Path path = pet.getNavigation().createPath(bed, 1);
        return path != null && path.canReach();
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
