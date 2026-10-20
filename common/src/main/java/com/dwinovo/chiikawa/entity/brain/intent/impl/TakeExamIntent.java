package com.dwinovo.chiikawa.entity.brain.intent.impl;

import com.dwinovo.chiikawa.entity.brain.intent.IntentCategory;
import com.dwinovo.chiikawa.entity.brain.intent.IntentCheck;
import com.dwinovo.chiikawa.entity.brain.intent.IntentContext;
import com.dwinovo.chiikawa.entity.brain.intent.PetIntent;
import com.dwinovo.chiikawa.entity.brain.intent.PetIntents;
import com.dwinovo.chiikawa.init.InitActivity;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.schedule.Activity;

/**
 * Sits a licence exam at the desk its owner signed it up at: that day, until the exam
 * closes. The desk is where it goes, so it starts only on a desk within the anchor's reach, as a
 * slip is taken, and a pet at heel sits it while its owner stays near the desk. Once it is in
 * its chair it has nowhere left to go.
 */
public final class TakeExamIntent implements PetIntent {
    /** Ahead of taking a slip or getting on with work: the owner paid for this. */
    @Override
    public ResourceLocation id() {
        return PetIntents.TAKE_EXAM;
    }

    @Override
    public IntentCategory category() {
        return IntentCategory.EXAM;
    }

    @Override
    public Activity activity() {
        return InitActivity.TAKE_EXAM.get();
    }

    @Override
    public IntentCheck canRun(IntentContext ctx) {
        return ctx.examDesk().isPresent() ? IntentCheck.OK : IntentCheck.fail("no_desk");
    }

    /** Kept at until it gets up: a pet that has just handed its paper in is still in its chair. */
    @Override
    public IntentCheck canContinue(IntentContext ctx) {
        return ctx.seatedAtExam() ? IntentCheck.OK : canRun(ctx);
    }

    @Override
    public Optional<GlobalPos> destination(IntentContext ctx) {
        return ctx.seatedAtExam() ? Optional.empty() : ctx.examDesk();
    }
}
