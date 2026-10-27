package com.dwinovo.chiikawa.entity.brain.errand.work;

import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.pastime.Pastime;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.task.PetTask;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * One kind of work, as its errand file defines it: which action does it, what kind of errand it is, what
 * it counts towards a slip, what the pet must meet before it sets off and what it is done to. Everything
 * else about the errand is its {@link com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings}.
 *
 * @param action what the pet does
 * @param category what kind of errand it is, which the owner's directive permits or forbids
 * @param counter the work counter its steps report, which a slip that counts it prefers the errand for
 * @param requires further conditions, checked in order before the action's own
 * @param target what the action is done to, for an action that takes one
 * @param pastime when a pet passes the time with it and who does, for a work that is a pastime and for no other
 * @param <P> the shape of the action's plans
 * @param <T> the shape of the action's tuning
 */
public record Work<P extends Plan, T>(WorkAction<P, T> action, ErrandCategory category,
                                      Optional<ResourceLocation> counter, List<WorkRequirement> requires,
                                      Optional<WorkTarget> target, Optional<Pastime> pastime) {
    public Work {
        requires = List.copyOf(requires);
    }

    /** @return whether the pet carries a slip that counts this work */
    public boolean countedBy(PetView view) {
        return counter.flatMap(count -> view.slip().map(PetTask::counter).filter(count::equals)).isPresent();
    }

    /** @return the target, which is of the kind the action takes: {@link WorkAction#refuses} has seen to that when the file is read */
    public <K extends WorkTarget> K targetOf(Class<K> kind) {
        return kind.cast(target.orElseThrow());
    }

    /** @return what is wrong with this work that a file cannot be read for, or nothing */
    Optional<String> problem() {
        Optional<String> refused = action.refuses(target).or(() -> action.refusesPastime(pastime));
        if (refused.isPresent()) {
            return refused;
        }
        if ((category == ErrandCategory.PASTIME) != pastime.isPresent()) {
            return Optional.of(category == ErrandCategory.PASTIME ? "a pastime needs a cast" : "only a pastime has a cast");
        }
        if (pastime.filter(found -> found.cast().isEmpty()).isPresent()) {
            return Optional.of("a pastime with nobody in it is never done");
        }
        return requires.stream().flatMap(requirement -> requirement.problemWith(this).stream()).findFirst();
    }
}
