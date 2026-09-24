package com.dwinovo.chiikawa.platform.services;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public interface IEntityHelper {
    void registerToEventBus(Object eventBus);

    void registerAttributes();

    void registerSpawnPlacements();

    /**
     * Moves an entity to a spot in another level, as a portal would but without one, and
     * returns what arrived there, or null if nothing did. 1.20.2 has no transition to hand
     * {@code changeDimension}; each loader has its own way to say where the entity lands.
     */
    Entity changeDimension(Entity entity, ServerLevel destination, Vec3 position, float yRot, float xRot);
}
