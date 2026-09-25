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
import net.minecraft.world.level.levelgen.RandomSupport;

/**
 * The rules of a labor board's daily slips, kept free of the block entity so they can be
 * tested on their own.
 */
public final class BoardSlips {
    /** Gives every slip a roll of its own, so a board that grows keeps the slips it had. */
    private static final long SLIP_SEED_STEP = 0x632BE59BD9B4E019L;
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
     * @param levels what each level of board puts up
     * @param level the board's level, which decides how many slips and which kinds
     * @return the day's slips, none when no types are loaded
     */
    public static List<BoardSlot> roll(long seed, SortedMap<ResourceLocation, PetTaskType> types, BoardLevels levels,
            int level) {
        return topUp(List.of(), seed, types, levels, level);
    }

    /**
     * Puts up whatever slips a board of this level is short of, leaving the ones already
     * on it alone: an upgrade paid for at noon adds a slip rather than sweeping away the
     * ones pets are out working on. Each slip rolls from its own seed, so the slips a
     * board has had all day stay exactly what they were.
     *
     * @param slots what is on the board already
     * @return the board's slips, the old ones first
     */
    public static List<BoardSlot> topUp(List<BoardSlot> slots, long seed,
            SortedMap<ResourceLocation, PetTaskType> types, BoardLevels levels, int level) {
        int wanted = levels.slipsAt(level);
        if (slots.size() >= wanted) {
            return slots;
        }
        List<Map.Entry<ResourceLocation, PetTaskType>> offered = types.entrySet().stream()
            .filter(entry -> entry.getValue().minLevel() <= levels.clamp(level))
            .toList();
        int totalWeight = offered.stream().mapToInt(entry -> entry.getValue().weight()).sum();
        if (totalWeight == 0) {
            return slots;
        }
        List<BoardSlot> filled = new ArrayList<>(slots);
        for (int i = slots.size(); i < wanted; i++) {
            filled.add(rollOne(seed, i, offered, totalWeight));
        }
        return List.copyOf(filled);
    }

    /**
     * Which of a board's slips still hang on it, a bit for each place from the lowest up:
     * every one nobody has taken down. A slip a pet is on its way to is still there to see
     * until the pet gets there.
     */
    public static int hanging(List<BoardSlot> slots) {
        int hanging = 0;
        for (int i = 0; i < slots.size(); i++) {
            if (!slots.get(i).claimed()) {
                hanging |= 1 << i;
            }
        }
        return hanging;
    }

    /**
     * The slip at one place on the board: the same one every time it is worked out. The
     * seed is stirred before it is used, as the game stirs a seed it makes from a position:
     * the game's random numbers start out alike from seeds that are alike, and a board's
     * seed and its neighbour's are alike, so unstirred, boards side by side put up much the
     * same slips.
     */
    private static BoardSlot rollOne(long seed, int index,
            List<Map.Entry<ResourceLocation, PetTaskType>> offered, int totalWeight) {
        RandomSource random = RandomSource.create(RandomSupport.mixStafford13(seed + index * SLIP_SEED_STEP));
        int pick = random.nextInt(totalWeight);
        for (Map.Entry<ResourceLocation, PetTaskType> entry : offered) {
            pick -= entry.getValue().weight();
            if (pick < 0) {
                return BoardSlot.open(entry.getValue().roll(entry.getKey(), random));
            }
        }
        throw new IllegalStateException("slip weights do not add up to " + totalWeight);
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
