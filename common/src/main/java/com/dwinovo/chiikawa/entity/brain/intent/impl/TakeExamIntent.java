package com.dwinovo.chiikawa.entity.brain.intent.impl;

import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.entity.brain.intent.IntentCategory;
import com.dwinovo.chiikawa.entity.brain.intent.IntentCheck;
import com.dwinovo.chiikawa.entity.brain.intent.IntentContext;
import com.dwinovo.chiikawa.entity.brain.intent.PetIntent;
import com.dwinovo.chiikawa.entity.brain.intent.PetIntents;
import com.dwinovo.chiikawa.init.InitActivity;
import java.util.Optional;
import java.util.function.BiPredicate;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.schedule.Activity;

/**
 * Sits a licence exam at the nearest labor board: on exam day, in working hours, when the
 * pet may sit, has made up its mind to, and there is a seat free. Starts only on a board
 * within the anchor's reach, as a slip is taken, so a pet at heel sits it only when its
 * owner brings it near a board.
 */
public final class TakeExamIntent implements PetIntent {
    /** Ahead of taking a slip: exam day is the day for the exam. */
    private static final float SCORE = 0.72F;

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
        return check(ctx.examBoard(), ctx, PetAnchor::withinReach, "out_of_reach");
    }

    @Override
    public IntentCheck canContinue(IntentContext ctx) {
        return check(ctx.examBoard(), ctx, PetAnchor::withinLeash, "out_of_leash");
    }

    @Override
    public float score(IntentContext ctx) {
        return SCORE;
    }

    static IntentCheck check(Optional<GlobalPos> board, IntentContext ctx,
            BiPredicate<PetAnchor, GlobalPos> inRange, String outOfRange) {
        return board
            .map(pos -> inRange.test(ctx.anchor(), pos) ? IntentCheck.OK : IntentCheck.fail(outOfRange))
            .orElseGet(() -> IntentCheck.fail("no_board"));
    }
}
