package com.dwinovo.chiikawa.entity.brain.errand.impl;

import com.dwinovo.chiikawa.entity.brain.errand.Check;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.view.PerceivedTargets;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.schedule.Activity;

/**
 * Acts on one sensed target, such as a crop to harvest or a chest to fill. Starts
 * while its requirements hold and the sensor remembers a target within the anchor's
 * reach.
 *
 * @param <T> the shape of its tuning
 */
public final class TargetErrand<T> extends LegacyErrand<Plan, T> {
    private final Heading toTarget;
    private final Function<PerceivedTargets, Optional<GlobalPos>> target;
    private final String missingReason;
    private final List<Requirement> requirements;
    private final Optional<ResourceLocation> workCounter;

    /**
     * @param target which remembered target this errand acts on
     * @param missingReason failure reason when there is no such target
     * @param requirements further conditions, checked in order before the target
     * @param workCounter the work counter its behaviors report, see {@link com.dwinovo.chiikawa.entity.brain.errand.ErrandType#workCounter}
     */
    public TargetErrand(ResourceLocation id, ErrandCategory category, Codec<T> tuning, Supplier<Activity> activity,
            Function<PerceivedTargets, Optional<GlobalPos>> target, String missingReason,
            List<Requirement> requirements, Optional<ResourceLocation> workCounter) {
        super(id, category, tuning, activity);
        this.toTarget = new Heading(view -> target.apply(view.targets()));
        this.target = target;
        this.missingReason = missingReason;
        this.requirements = List.copyOf(requirements);
        this.workCounter = workCounter;
    }

    @Override
    protected Check canStart(PetView view, T tuning) {
        for (Requirement requirement : requirements) {
            Check check = requirement.check(view);
            if (!check.ok()) {
                return check;
            }
        }
        return target.apply(view.targets()).isPresent() ? Check.OK : Check.fail(missingReason);
    }

    @Override
    protected Plan plan(PetView view, T tuning) {
        return toTarget;
    }

    @Override
    public Optional<ResourceLocation> workCounter() {
        return workCounter;
    }
}
