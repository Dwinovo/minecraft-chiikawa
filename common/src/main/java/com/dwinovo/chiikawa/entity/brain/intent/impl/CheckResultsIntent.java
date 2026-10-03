package com.dwinovo.chiikawa.entity.brain.intent.impl;

import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.entity.brain.intent.IntentCategory;
import com.dwinovo.chiikawa.entity.brain.intent.IntentCheck;
import com.dwinovo.chiikawa.entity.brain.intent.IntentContext;
import com.dwinovo.chiikawa.entity.brain.intent.PetIntent;
import com.dwinovo.chiikawa.entity.brain.intent.PetIntents;
import com.dwinovo.chiikawa.init.InitActivity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.schedule.Activity;

/**
 * Goes back to the exam desk it sat an exam at, the morning after, to see how it did. A
 * pet that cannot get there by noon hears its results wherever it is instead; see
 * {@link com.dwinovo.chiikawa.qualification.PetExams#upkeep}.
 */
public final class CheckResultsIntent implements PetIntent {
    /** As keen as sitting the exam was: nothing else is on its mind that morning. */
    private static final float SCORE = 0.72F;

    @Override
    public ResourceLocation id() {
        return PetIntents.CHECK_RESULTS;
    }

    @Override
    public IntentCategory category() {
        return IntentCategory.EXAM;
    }

    @Override
    public Activity activity() {
        return InitActivity.CHECK_RESULTS.get();
    }

    @Override
    public IntentCheck canRun(IntentContext ctx) {
        return TakeExamIntent.check(ctx.resultsDesk(), ctx, PetAnchor::withinReach, "out_of_reach");
    }

    @Override
    public IntentCheck canContinue(IntentContext ctx) {
        return TakeExamIntent.check(ctx.resultsDesk(), ctx, PetAnchor::withinLeash, "out_of_leash");
    }

    @Override
    public float score(IntentContext ctx) {
        return SCORE;
    }
}
