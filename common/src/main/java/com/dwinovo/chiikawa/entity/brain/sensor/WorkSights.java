package com.dwinovo.chiikawa.entity.brain.sensor;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.rest.SubjectRests;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkErrand;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkTarget;
import com.dwinovo.chiikawa.entity.job.JobWork;
import com.dwinovo.chiikawa.init.InitMemory;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

/**
 * What the work sensor sees, as the sensor that writes it and the view that shows it to the errands read it: the
 * blocks of work in the {@code WORK_SIGHTS} memory, and which of them a pet is not resting from. The one place that
 * says so; only {@link PetWorkSensor} writes the memory.
 *
 * <p>The sensor remembers more than one block for a work, nearest first, the first being the one it chose. What the
 * view shows is the first of them that still is what it was remembered as.
 */
public final class WorkSights {
    /**
     * A work that is done to blocks the sensor finds.
     *
     * @param errand the id of the errand, which is what the sights and the rests are kept under
     * @param kind what makes a block one the work is done to
     */
    public record BlockWork(ResourceLocation errand, WorkTarget.BlockTarget kind) {
    }

    private WorkSights() {
    }

    /** @return the works of the pet's job that are done to blocks the sensor finds, in the order the job lists them */
    public static List<BlockWork> blockWorksOf(AbstractPet pet) {
        return JobWork.errandsOf(pet).stream()
            .filter(WorkErrand.class::isInstance).map(errand -> (WorkErrand<?, ?>) errand)
            .flatMap(work -> work.blocks().map(kind -> new BlockWork(work.id(), kind)).stream()).toList();
    }

    /** @return the blocks the sensor remembers for the work, the one it chose and then those it found after it */
    public static List<BlockPos> remembered(AbstractPet pet, ResourceLocation errand) {
        return pet.getBrain().getMemory(InitMemory.WORK_SIGHTS.get()).map(sights -> sights.getOrDefault(errand, List.of()))
            .orElse(List.of());
    }

    /** @return whether the errand is resting from the block, which the ledger writes and this only reads */
    public static boolean resting(AbstractPet pet, ResourceLocation errand, BlockPos pos, long now) {
        return pet.getBrain().getMemory(InitMemory.SUBJECT_RESTS.get())
            .filter(rests -> rests.remaining(errand, SubjectRests.subject(pos), now) > 0).isPresent();
    }

    /**
     * @return the first block the sensor remembers for the work that still is one of its kind, where it is now, and that
     *         the errand is not resting from: a pet that has just reaped a crop is shown the next one at once
     */
    public static Optional<GlobalPos> seenBy(AbstractPet pet, BlockWork work) {
        if (!(pet.level() instanceof ServerLevel level)) {
            return Optional.empty();
        }
        long now = level.getGameTime();
        return remembered(pet, work.errand()).stream()
            .filter(pos -> work.kind().holds(level, pet, pos) && !resting(pet, work.errand(), pos, now)).findFirst()
            .map(pos -> GlobalPos.of(level.dimension(), pos));
    }
}
