package com.dwinovo.chiikawa.entity.brain.errand.run;

import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * The errand a pet is on, as the rest of the game may look at it. Not saved: a loaded pet decides again.
 *
 * <p>Keeps the settings the errand started with, so a data pack reload changes the next errand and
 * never the rests or the time limit of the one under way.
 *
 * @param errand the errand's id
 * @param plan what the pet set out to do
 * @param settings how the errand goes, as it was when it started
 * @param stepIndex how many steps of the routine have begun before the one it is at
 * @param startedAt the game time it started
 * @param tags what the step it is at makes of the pet
 */
public record Running(ResourceLocation errand, Plan plan, ErrandSettings<?> settings, int stepIndex, long startedAt,
                      Set<StepTag> tags) {
    public Running {
        tags = Set.copyOf(tags);
    }

    /** @return how much the errand matters, as its settings said when it started */
    public int priority() {
        return settings.priority();
    }

    /** @return this errand, at the step with this index */
    Running at(int stepIndex, Set<StepTag> tags) {
        return new Running(errand, plan, settings, stepIndex, startedAt, tags);
    }
}
