package com.dwinovo.chiikawa.entity.brain;

import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandType;
import com.dwinovo.chiikawa.entity.brain.errand.decide.Permits;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkAction;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkErrand;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

/**
 * Which progress memories a pet is to erase because what they belong to is not available to it: the memories a code
 * errand holds, while the pet's job does not offer it or its directive does not permit it, and the memories an action
 * holds, while no errand of the pet that is made with that action can be pursued. The actions are asked, not the
 * errands loaded, so what a work file did or did not load changes nothing about what is forgotten.
 */
final class Forgetting {
    private Forgetting() {
    }

    /**
     * @param errands every errand there is; the progress of a work errand is its action's, so only the code errands
     *                answer for themselves
     * @param actions every action there is
     * @param offered the errands the pet's job offers
     * @param permits what the pet's directive permits
     * @return the memories to erase
     */
    static Set<MemoryModuleType<?>> of(Collection<ErrandType<?, ?>> errands, Collection<WorkAction<?, ?>> actions,
            List<ErrandType<?, ?>> offered, Permits permits) {
        Set<MemoryModuleType<?>> forgotten = new HashSet<>();
        for (ErrandType<?, ?> type : errands) {
            if (!pursuable(type, offered, permits)) {
                forgotten.addAll(type.forgetWhenUnavailable());
            }
        }
        for (WorkAction<?, ?> action : actions) {
            boolean used = offered.stream().anyMatch(type -> type instanceof WorkErrand<?, ?> work
                && work.action() == action && pursuable(type, offered, permits));
            if (!used) {
                forgotten.addAll(action.forgetWhenUnavailable());
            }
        }
        return forgotten;
    }

    private static boolean pursuable(ErrandType<?, ?> type, List<ErrandType<?, ?>> offered, Permits permits) {
        return PetMind.isOffered(offered, type.id()) && permits.allows(type.category())
            && ErrandSettings.of(type.id()).isPresent();
    }
}
