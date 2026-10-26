package com.dwinovo.chiikawa.entity.job;

import com.dwinovo.chiikawa.entity.brain.errand.ErrandType;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkErrand;
import com.dwinovo.chiikawa.entity.job.api.PetCapability;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;

/**
 * What a job works with: its tool, and what its work uses up or shoots, see
 * {@link com.dwinovo.chiikawa.entity.brain.errand.work.WorkAction#materials}. A pet does not give these as presents,
 * so the farmer that is forever planting is not forever giving seeds.
 */
public final class WorkItems {
    private WorkItems() {
    }

    /**
     * @param job a job
     * @param errands the errands a pet of that job considers, which are those of its work that count
     * @return whether a stack is something a pet of that job works with
     */
    public static Predicate<ItemStack> of(PetCapability job, Collection<ErrandType<?, ?>> errands) {
        List<Predicate<ItemStack>> materials = errands.stream().filter(WorkErrand.class::isInstance)
            .map(errand -> ((WorkErrand<?, ?>) errand).action().materials()).toList();
        return stack -> job.toolTag().filter(stack::is).isPresent() || materials.stream().anyMatch(made -> made.test(stack));
    }
}
