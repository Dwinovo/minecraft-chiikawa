package com.dwinovo.chiikawa.entity.brain.intent.impl;

import com.dwinovo.chiikawa.entity.brain.intent.IntentCategory;
import com.dwinovo.chiikawa.entity.brain.intent.IntentCheck;
import com.dwinovo.chiikawa.entity.brain.intent.IntentContext;
import com.dwinovo.chiikawa.entity.brain.intent.IntentRequirement;
import com.dwinovo.chiikawa.entity.brain.intent.PerceivedTargets;
import com.dwinovo.chiikawa.entity.brain.intent.PetIntent;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.schedule.Activity;

/**
 * Acts on one sensed target, such as a crop to harvest or an item to pick up. Runs
 * while its requirements hold and the sensor remembers a target within the anchor's
 * reach.
 */
public final class TargetIntent implements PetIntent {
    private final Identifier id;
    private final IntentCategory category;
    private final Supplier<Activity> activity;
    private final Function<PerceivedTargets, Optional<GlobalPos>> target;
    private final float score;
    private final String missingReason;
    private final List<IntentRequirement> requirements;
    private final Optional<Identifier> workCounter;

    /**
     * @param target which remembered target this intent acts on
     * @param missingReason failure reason when there is no such target
     * @param requirements further conditions, checked in order before the target
     * @param workCounter the work counter its behaviors report, see {@link PetIntent#workCounter}
     */
    public TargetIntent(Identifier id, IntentCategory category, Supplier<Activity> activity,
            Function<PerceivedTargets, Optional<GlobalPos>> target, float score, String missingReason,
            List<IntentRequirement> requirements, Optional<Identifier> workCounter) {
        this.id = id;
        this.category = category;
        this.activity = activity;
        this.target = target;
        this.score = score;
        this.missingReason = missingReason;
        this.requirements = List.copyOf(requirements);
        this.workCounter = workCounter;
    }

    @Override
    public Identifier id() {
        return id;
    }

    @Override
    public IntentCategory category() {
        return category;
    }

    @Override
    public Activity activity() {
        return activity.get();
    }

    @Override
    public IntentCheck canRun(IntentContext ctx) {
        for (IntentRequirement requirement : requirements) {
            IntentCheck check = requirement.check(ctx);
            if (!check.ok()) {
                return check;
            }
        }
        return target.apply(ctx.targets())
            .map(pos -> ctx.anchor().withinReach(pos) ? IntentCheck.OK : IntentCheck.fail("out_of_reach"))
            .orElseGet(() -> IntentCheck.fail(missingReason));
    }

    @Override
    public float score(IntentContext ctx) {
        return score;
    }

    @Override
    public Optional<Identifier> workCounter() {
        return workCounter;
    }
}
