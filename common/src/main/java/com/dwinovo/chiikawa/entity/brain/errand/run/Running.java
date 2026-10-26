package com.dwinovo.chiikawa.entity.brain.errand.run;

import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandType;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * The errand a pet is on, as the rest of the game may look at it. Not saved: a loaded pet decides again.
 *
 * <p>Keeps the type and the settings the errand started with, so a data pack reload changes the next errand
 * and never the rests, the time limit or the way to go on of the one under way. Whatever asks if it is the same
 * errand as another asks by id: a reload makes new types.
 *
 * @param type the errand, as it was when it started
 * @param plan what the pet set out to do
 * @param settings how the errand goes, as it was when it started
 * @param stepIndex how many steps of the routine have begun before the one it is at
 * @param startedAt the game time it started
 * @param tags what the step it is at makes of the pet
 */
public record Running(ErrandType<?, ?> type, Plan plan, ErrandSettings<?> settings, int stepIndex, long startedAt,
                      Set<StepTag> tags) {
    public Running {
        tags = Set.copyOf(tags);
    }

    /** @return the errand's id */
    public ResourceLocation errand() {
        return type.id();
    }

    /** @return how much the errand matters, as its settings said when it started */
    public int priority() {
        return settings.priority();
    }

    /** @return this errand, at the step with this index */
    Running at(int stepIndex, Set<StepTag> tags) {
        return new Running(type, plan, settings, stepIndex, startedAt, tags);
    }
}
