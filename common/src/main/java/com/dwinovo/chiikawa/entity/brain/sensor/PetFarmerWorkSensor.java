package com.dwinovo.chiikawa.entity.brain.sensor;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.rest.SubjectRests;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.init.InitRegistry;
import com.dwinovo.chiikawa.utils.BlockSearch;
import com.google.common.collect.ImmutableSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;

/**
 * Single farmer work sensor — replaces the former plant / harvest / container
 * sensors, and also finds weeds and mushrooms. Three perf properties matter here:
 *
 * <ol>
 *   <li><b>No pathfinding in the scan.</b> Candidates are chosen by cheap block
 *       checks only. Whether the way to one can be found is the errand's walk to find
 *       out; a block the pet could not get to is left alone by that errand for a while,
 *       see {@link SubjectRests}, and this scan skips it.</li>
 *   <li><b>Cheap re-validation.</b> Existing targets are re-checked with a block
 *       lookup, never a re-path.</li>
 *   <li><b>One pass.</b> A single spiral collects all needed targets at once, and
 *       the whole scan is skipped when every needed kind still has a block. The spiral is at most
 *       {@code (2 * MAX_RADIUS + 1)^2 * (2 * VERTICAL_RANGE + 1)} blocks, which bounds what is kept.</li>
 * </ol>
 *
 * The five targets, a crop to harvest, soil to plant, a container to fill, a weed and a
 * mushroom, are each found on their own, and what the sensor keeps is the field and not one block of it: every block
 * of a kind in reach, nearest first, the first in the kind's own memory and the rest in {@link FarmSights}. They are kept
 * until they stop holding, so a pet that has reaped a crop knows the next at once and the errands that yield to its
 * work are not let through in the gap before the next scan. The order the errands work them in is theirs to keep, see
 * the {@code yields_to} of their settings. The sensor only perceives, and is the only one that writes them: it keeps
 * scanning whatever the owner's directive, the pet's ownership or the time of day, and the errands decide whether a
 * target is used.
 */
public class PetFarmerWorkSensor extends Sensor<AbstractPet> {
    private static final int MAX_RADIUS = 5;
    private static final int VERTICAL_RANGE = 1;
    /** Backoff cap: each consecutive empty search adds this many ticks, up to MAX_EMPTY_STREAK steps. */
    private static final int BACKOFF_STEP_TICKS = 30;
    // Caps idle search interval at ~3*30 = 90t (~4.5s) so a standing-still pet still
    // notices freshly-placed work reasonably fast.
    private static final int MAX_EMPTY_STREAK = 3;
    /**
     * A remembered target this far from the pet is forgotten. Errands ignore targets
     * outside their anchor's reach, and a kept target blocks the scan for another one,
     * so a pet that wandered off must not stay stuck on targets it no longer uses.
     */
    private static final double FORGET_DISTANCE = MAX_RADIUS * 2.0;

    // (6) Adaptive backoff state (per-pet: each brain owns its own sensor instance).
    private long nextSearchTime = 0L;
    private int emptySearchStreak = 0;
    private BlockPos lastSearchCenter = null;

    /**
     * Memory types managed by this sensor.
     * @return required memory types
     */
    public Set<MemoryModuleType<?>> requires() {
        return Stream.concat(Stream.of(FarmTarget.values()).<MemoryModuleType<?>>map(FarmTarget::memory),
            Stream.of(InitMemory.FARM_SIGHTS.get())).collect(ImmutableSet.toImmutableSet());
    }

    @Override
    protected void doTick(ServerLevel level, AbstractPet pet) {
        Brain<AbstractPet> brain = pet.getBrain();
        // Only farmers use these targets; skip the scan for other jobs.
        if (pet.getPetJobId() != InitRegistry.FARMER_ID) {
            for (FarmTarget target : FarmTarget.values()) {
                brain.eraseMemory(target.memory());
            }
            brain.eraseMemory(InitMemory.FARM_SIGHTS.get());
            return;
        }
        long now = level.getGameTime();

        // (2) Re-validate existing targets cheaply — block checks only, no pathfinding.
        for (FarmTarget target : FarmTarget.values()) {
            revalidate(level, pet, target, now);
        }

        // Decide what still needs finding.
        List<FarmTarget> needed = Stream.of(FarmTarget.values())
            .filter(target -> target.remembered(pet).isEmpty() && target.worthFinding(pet)).toList();
        if (needed.isEmpty()) {
            return; // everything satisfied — skip the scan entirely
        }

        // (6) Adaptive backoff: if recent searches from this spot found nothing,
        // search progressively less often. Reset the moment the pet moves.
        BlockPos center = pet.blockPosition();
        if (!center.equals(lastSearchCenter)) {
            lastSearchCenter = center;
            emptySearchStreak = 0;
            nextSearchTime = 0L;
        }
        if (now < nextSearchTime) {
            return;
        }

        // (1)(3) One spiral pass, cheap predicates only, skipping blocks the errand is resting from. A corner is
        // visited from two sides of the spiral, hence the sets.
        List<Set<BlockPos>> found = needed.stream().<Set<BlockPos>>map(target -> new LinkedHashSet<>()).toList();
        BlockSearch.spiralVisit(center, MAX_RADIUS, VERTICAL_RANGE, pos -> {
            for (int i = 0; i < found.size(); i++) {
                FarmTarget target = needed.get(i);
                if (target.holds(level, pet, pos) && !target.resting(pet, pos, now)) {
                    found.get(i).add(pos.immutable());
                }
            }
            return false;
        });

        boolean foundAny = false;
        for (int i = 0; i < found.size(); i++) {
            if (!found.get(i).isEmpty()) {
                remember(pet, needed.get(i), List.copyOf(found.get(i)));
                foundAny = true;
            }
        }

        // Found work → search eagerly again; found nothing → back off a step.
        if (foundAny) {
            emptySearchStreak = 0;
            nextSearchTime = 0L;
        } else {
            emptySearchStreak = Math.min(emptySearchStreak + 1, MAX_EMPTY_STREAK);
            nextSearchTime = now + (long) emptySearchStreak * BACKOFF_STEP_TICKS;
        }
    }

    /** Keeps the blocks that still are what they were remembered as, near the pet and not rested from. */
    private static void revalidate(ServerLevel level, AbstractPet pet, FarmTarget target, long now) {
        List<BlockPos> remembered = target.remembered(pet);
        List<BlockPos> holding = remembered.stream().filter(pos -> pos.closerThan(pet.blockPosition(), FORGET_DISTANCE)
            && target.holds(level, pet, pos) && !target.resting(pet, pos, now)).toList();
        if (holding.size() != remembered.size()) {
            remember(pet, target, holding);
        }
    }

    /** The first block is the one the sensor chose, in the kind's own memory; the others are the sights after it. */
    private static void remember(AbstractPet pet, FarmTarget target, List<BlockPos> blocks) {
        Brain<AbstractPet> brain = pet.getBrain();
        if (blocks.isEmpty()) {
            brain.eraseMemory(target.memory());
        } else {
            brain.setMemory(target.memory(), blocks.get(0));
        }
        FarmSights sights = brain.getMemory(InitMemory.FARM_SIGHTS.get()).orElse(FarmSights.NONE);
        brain.setMemory(InitMemory.FARM_SIGHTS.get(), sights.with(target, blocks.isEmpty() ? blocks : blocks.subList(1, blocks.size())));
    }
}
