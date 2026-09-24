package com.dwinovo.chiikawa.entity.brain.intent.impl;

import com.dwinovo.chiikawa.entity.brain.intent.IntentCategory;
import com.dwinovo.chiikawa.entity.brain.intent.IntentCheck;
import com.dwinovo.chiikawa.entity.brain.intent.IntentContext;
import com.dwinovo.chiikawa.entity.brain.intent.PetIntent;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.schedule.Activity;

/**
 * An intent that can always run at a fixed score, such as staying put or wandering;
 * whether it applies is left entirely to the permission table.
 */
public final class ConstantIntent implements PetIntent {
    private final Identifier id;
    private final IntentCategory category;
    private final Supplier<Activity> activity;
    private final float score;

    public ConstantIntent(Identifier id, IntentCategory category, Supplier<Activity> activity, float score) {
        this.id = id;
        this.category = category;
        this.activity = activity;
        this.score = score;
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
        return IntentCheck.OK;
    }

    @Override
    public float score(IntentContext ctx) {
        return score;
    }
}
