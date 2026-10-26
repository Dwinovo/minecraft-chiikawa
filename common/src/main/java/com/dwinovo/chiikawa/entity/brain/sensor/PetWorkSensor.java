package com.dwinovo.chiikawa.entity.brain.sensor;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.rest.SubjectRests;
import com.dwinovo.chiikawa.entity.brain.sensor.WorkSights.BlockWork;
import com.dwinovo.chiikawa.entity.job.JobWork;
import com.dwinovo.chiikawa.entity.job.Sight;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.utils.BlockSearch;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;

/**
 * The one work sensor: finds the blocks the works of the pet's job are done to, such as a crop to harvest, soil to
 * plant, a container to fill, a weed and a mushroom. How far and how often it looks is the job's {@link Sight}. Three
 * perf properties matter here:
 *
 * <ol>
 *   <li><b>No pathfinding in the scan.</b> Candidates are chosen by cheap block checks only. Whether the way to one can
 *       be found is the errand's walk to find out; a block the pet could not get to is left alone by that errand for a
 *       while, see {@link SubjectRests}, and this scan skips it.</li>
 *   <li><b>Cheap re-validation.</b> Existing targets are re-checked with a block lookup, never a re-path.</li>
 *   <li><b>One pass.</b> A single spiral collects all needed targets at once, and the whole scan is skipped when every
 *       needed work still has a block. The spiral is at most {@code (2 * radius + 1)^2 * (2 * vertical range + 1)}
 *       blocks, which bounds what is kept.</li>
 * </ol>
 *
 * What the sensor keeps is the field and not one block of it: every block of a work in reach, nearest first, the first
 * being the one it chose, in {@code WORK_SIGHTS}. They are kept until they stop holding, so a pet that has reaped a crop
 * knows the next at once and the errands that yield to its work are not let through in the gap before the next scan. The
 * order the errands work them in is theirs to keep, see the {@code yields_to} of their settings. The sensor only
 * perceives, and is the only one that writes them: it keeps scanning whatever the owner's directive, the pet's ownership
 * or the time of day, and the errands decide whether a target is used. It looks only for a pet whose job has work done
 * to blocks.
 */
public class PetWorkSensor extends Sensor<AbstractPet> {
    // Adaptive backoff state (per-pet: each brain owns its own sensor instance).
    private long nextSearchTime = 0L;
    private int emptySearchStreak = 0;
    private BlockPos lastSearchCenter = null;

    /**
     * Memory types managed by this sensor.
     * @return required memory types
     */
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(InitMemory.WORK_SIGHTS.get());
    }

    @Override
    protected void doTick(ServerLevel level, AbstractPet pet) {
        Brain<AbstractPet> brain = pet.getBrain();
        List<BlockWork> works = WorkSights.blockWorksOf(pet);
        Optional<Sight> job = JobWork.sightOf(pet);
        // Only a pet whose job has work done to blocks looks for them; skip the scan for other jobs.
        if (works.isEmpty() || job.isEmpty()) {
            brain.eraseMemory(InitMemory.WORK_SIGHTS.get());
            return;
        }
        Sight sight = job.get();
        long now = level.getGameTime();

        // Re-validate existing targets cheaply: block checks only, no pathfinding.
        for (BlockWork work : works) {
            revalidate(level, pet, work, sight, now);
        }

        // Decide what still needs finding.
        List<BlockWork> needed = works.stream()
            .filter(work -> WorkSights.remembered(pet, work.errand()).isEmpty() && work.kind().worthFinding(pet)).toList();
        if (needed.isEmpty()) {
            return; // everything satisfied — skip the scan entirely
        }

        // Adaptive backoff: if recent searches from this spot found nothing, search progressively less often. Reset the
        // moment the pet moves.
        BlockPos center = pet.blockPosition();
        if (!center.equals(lastSearchCenter)) {
            lastSearchCenter = center;
            emptySearchStreak = 0;
            nextSearchTime = 0L;
        }
        if (now < nextSearchTime) {
            return;
        }

        // One spiral pass, cheap predicates only, skipping blocks the errand is resting from. A corner is visited from
        // two sides of the spiral, hence the sets.
        List<Set<BlockPos>> found = needed.stream().<Set<BlockPos>>map(work -> new LinkedHashSet<>()).toList();
        BlockSearch.spiralVisit(center, sight.radius(), sight.verticalRange(), pos -> {
            for (int i = 0; i < found.size(); i++) {
                BlockWork work = needed.get(i);
                if (work.kind().holds(level, pet, pos) && !WorkSights.resting(pet, work.errand(), pos, now)) {
                    found.get(i).add(pos.immutable());
                }
            }
            return false;
        });

        boolean foundAny = false;
        for (int i = 0; i < found.size(); i++) {
            if (!found.get(i).isEmpty()) {
                remember(pet, needed.get(i).errand(), List.copyOf(found.get(i)));
                foundAny = true;
            }
        }

        // Found work → search eagerly again; found nothing → back off a step.
        if (foundAny) {
            emptySearchStreak = 0;
            nextSearchTime = 0L;
        } else {
            emptySearchStreak = Math.min(emptySearchStreak + 1, sight.emptyStreakLimit());
            nextSearchTime = now + (long) emptySearchStreak * sight.backoffStepTicks();
        }
    }

    /** Keeps the blocks that still are what they were remembered as, near the pet and not rested from. */
    private static void revalidate(ServerLevel level, AbstractPet pet, BlockWork work, Sight sight, long now) {
        List<BlockPos> remembered = WorkSights.remembered(pet, work.errand());
        List<BlockPos> holding = remembered.stream()
            .filter(pos -> pos.closerThan(pet.blockPosition(), sight.forgetDistance())
                && work.kind().holds(level, pet, pos) && !WorkSights.resting(pet, work.errand(), pos, now))
            .toList();
        if (holding.size() != remembered.size()) {
            remember(pet, work.errand(), holding);
        }
    }

    /** The first block is the one the sensor chose; the others are the sights after it. */
    private static void remember(AbstractPet pet, ResourceLocation errand, List<BlockPos> blocks) {
        Brain<AbstractPet> brain = pet.getBrain();
        Map<ResourceLocation, List<BlockPos>> sights = new HashMap<>(
            brain.getMemory(InitMemory.WORK_SIGHTS.get()).orElse(Map.of()));
        if (blocks.isEmpty()) {
            sights.remove(errand);
        } else {
            sights.put(errand, List.copyOf(blocks));
        }
        if (sights.isEmpty()) {
            brain.eraseMemory(InitMemory.WORK_SIGHTS.get());
        } else {
            brain.setMemory(InitMemory.WORK_SIGHTS.get(), Map.copyOf(sights));
        }
    }
}
