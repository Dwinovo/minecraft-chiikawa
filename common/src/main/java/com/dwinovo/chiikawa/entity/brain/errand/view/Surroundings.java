package com.dwinovo.chiikawa.entity.brain.errand.view;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

/**
 * What a pet can see of its surroundings within a few blocks, as its sensor last looked: somewhere to get out of the rain,
 * the creatures and the things lying about, and whether one of the creatures is a creeper about to go off. A few seconds
 * old at most, and not saved: the sensor looks again when the pet is back.
 *
 * @param shelter the nearest place the pet could stand out of the rain; looked for only while it rains
 * @param creatures the living things near, nearest first
 * @param items the things lying about, nearest first
 * @param primedCreeper whether one of the creatures is a creeper that has begun to swell
 */
public record Surroundings(Optional<BlockPos> shelter, List<Sighting<EntityType<?>>> creatures, List<Sighting<Item>> items,
                           boolean primedCreeper) {
    /** Nothing seen. */
    public static final Surroundings NONE = new Surroundings(Optional.empty(), List.of(), List.of(), false);

    public Surroundings {
        creatures = List.copyOf(creatures);
        items = List.copyOf(items);
    }

    /**
     * Something seen.
     *
     * @param id its entity id
     * @param where the block it was in
     * @param kind what kind of creature or item it is
     * @param <T> the kind of thing
     */
    public record Sighting<T>(UUID id, BlockPos where, T kind) {
    }
}
