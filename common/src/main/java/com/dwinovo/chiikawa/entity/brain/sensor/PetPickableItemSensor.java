package com.dwinovo.chiikawa.entity.brain.sensor;

import com.dwinovo.chiikawa.entity.AbstractPet;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.server.level.ServerLevel;
import java.util.Set;
import com.google.common.collect.ImmutableSet;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.init.InitTag;
import com.dwinovo.chiikawa.utils.Utils;
import net.minecraft.world.phys.AABB;
import java.util.Comparator;
import net.minecraft.world.entity.item.ItemEntity;
// Finds nearby pickable items.
public class PetPickableItemSensor extends Sensor<AbstractPet>{
    // Search range.
    private static final int VERTICAL_SEARCH_RANGE = 7;
    /**
     * Sensor interval.
     */
    public PetPickableItemSensor() {
        // Scan every 30 ticks.
        super(30);
    }
    /**
     * Memory types used by this sensor.
     * @return required memory types
     */
    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(InitMemory.PICKABLE_ITEM.get());
    }
    /**
     * Scan for pickable items.
     * @param level the server level
     * @param entity the pet entity
     */
    @SuppressWarnings("null")
    @Override
    protected void doTick(ServerLevel level, AbstractPet entity) {
        AABB aabb = entity.getBoundingBox().inflate(VERTICAL_SEARCH_RANGE, VERTICAL_SEARCH_RANGE, VERTICAL_SEARCH_RANGE);
        // Nearest reachable item. Sorting before the reachability check means the
        // pathfind usually runs for the nearest candidate only.
        ItemEntity target = level.getEntitiesOfClass(ItemEntity.class, aabb, ItemEntity::isAlive).stream()
                .filter(e -> !e.hasPickUpDelay())
                .filter(e -> e.getItem().is(InitTag.ENTITY_PICKABLE_ITEMS))
                .sorted(Comparator.comparingDouble(entity::distanceToSqr))
                .filter(e -> Utils.canReach(entity, e.blockPosition()))
                .findFirst()
                .orElse(null);
        if (target != null) {
            entity.getBrain().setMemory(InitMemory.PICKABLE_ITEM.get(), target);
        } else {
            entity.getBrain().eraseMemory(InitMemory.PICKABLE_ITEM.get());
        }
    }
}

