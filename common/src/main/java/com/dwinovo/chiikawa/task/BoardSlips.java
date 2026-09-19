package com.dwinovo.chiikawa.task;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.SortedMap;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

/**
 * The rules of a labor board's daily slips, kept free of the block entity so they can be
 * tested on their own.
 */
public final class BoardSlips {
    public static final int SLIPS_PER_DAY = 3;
    /** Wild pets only take what owned pets left: from this long after sunrise on. */
    public static final long WILD_CLAIM_DELAY = 12000L;
    /** How long a pet on its way holds a slip before it lapses. */
    public static final int RESERVATION_TICKS = 1200;

    private BoardSlips() {
    }

    /**
     * The same board on the same day rolls the same slips, whoever looks at it first.
     *
     * @param worldSeed the world seed
     * @param day the day number, counted in whole days of time of day
     * @param pos the board's position
     * @return the seed of that board's roll for that day
     */
    public static long seed(long worldSeed, long day, BlockPos pos) {
        return worldSeed ^ (day * 0x9E3779B97F4A7C15L) ^ (pos.asLong() * 0xC2B2AE3D27D4EB4FL);
    }

    /**
     * @param seed see {@link #seed}
     * @param types the loaded task types in id order
     * @return the day's slips, none when no types are loaded
     */
    public static List<BoardSlot> roll(long seed, SortedMap<ResourceLocation, PetTaskType> types) {
        int totalWeight = types.values().stream().mapToInt(PetTaskType::weight).sum();
        if (totalWeight == 0) {
            return List.of();
        }
        RandomSource random = RandomSource.create(seed);
        List<BoardSlot> slots = new ArrayList<>(SLIPS_PER_DAY);
        for (int i = 0; i < SLIPS_PER_DAY; i++) {
            int pick = random.nextInt(totalWeight);
            for (Map.Entry<ResourceLocation, PetTaskType> entry : types.entrySet()) {
                pick -= entry.getValue().weight();
                if (pick < 0) {
                    slots.add(BoardSlot.open(entry.getValue().roll(entry.getKey(), random)));
                    break;
                }
            }
        }
        return List.copyOf(slots);
    }

    /**
     * The slip a pet would take: the one it already holds, otherwise the first open one
     * for its capability. Wild pets only look once {@link #WILD_CLAIM_DELAY} has passed.
     *
     * @param slots the board's slips
     * @param capability the pet's capability id
     * @param pet the pet's UUID
     * @param wild whether the pet has no owner
     * @param timeOfDay ticks since sunrise
     * @param gameTime the current game time, for reservations
     * @return the slip's index
     */
    public static OptionalInt find(List<BoardSlot> slots, ResourceLocation capability, UUID pet, boolean wild,
            long timeOfDay, long gameTime) {
        for (int i = 0; i < slots.size(); i++) {
            BoardSlot slot = slots.get(i);
            if (slot.reservedBy(pet, gameTime) && slot.slip().capability().equals(capability)) {
                return OptionalInt.of(i);
            }
        }
        if (wild && timeOfDay < WILD_CLAIM_DELAY) {
            return OptionalInt.empty();
        }
        for (int i = 0; i < slots.size(); i++) {
            BoardSlot slot = slots.get(i);
            if (slot.slip().capability().equals(capability) && slot.openTo(pet, gameTime)) {
                return OptionalInt.of(i);
            }
        }
        return OptionalInt.empty();
    }
}
