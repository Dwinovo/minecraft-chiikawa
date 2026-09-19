package com.dwinovo.chiikawa.entity.brain.intent.impl;

import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.entity.brain.intent.IntentCategory;
import com.dwinovo.chiikawa.entity.brain.intent.IntentCheck;
import com.dwinovo.chiikawa.entity.brain.intent.IntentContext;
import com.dwinovo.chiikawa.entity.brain.intent.PetIntent;
import com.dwinovo.chiikawa.entity.brain.intent.PetIntents;
import com.dwinovo.chiikawa.init.InitActivity;
import java.util.function.BiPredicate;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.schedule.Activity;

/**
 * Walks to the nearest labor board and takes a slip for the pet's job. A pet carries one
 * slip at a time, and goes back for another once it has been paid.
 *
 * <p>It starts only on a board within the anchor's reach, so a following pet takes a slip
 * on the way, and keeps going while the board stays within the leash. A pet that could
 * not get its slip rests from the board for a while, see {@code TAKE_TASK_COOLDOWN}.
 */
public final class TakeTaskIntent implements PetIntent {
    private static final float SCORE = 0.7F;

    @Override
    public ResourceLocation id() {
        return PetIntents.TAKE_TASK;
    }

    @Override
    public IntentCategory category() {
        return IntentCategory.TAKE_TASK;
    }

    @Override
    public Activity activity() {
        return InitActivity.TAKE_TASK.get();
    }

    @Override
    public IntentCheck canRun(IntentContext ctx) {
        return check(ctx, PetAnchor::withinReach, "out_of_reach");
    }

    @Override
    public IntentCheck canContinue(IntentContext ctx) {
        return check(ctx, PetAnchor::withinLeash, "out_of_leash");
    }

    @Override
    public float score(IntentContext ctx) {
        return SCORE;
    }

    private static IntentCheck check(IntentContext ctx,
            BiPredicate<PetAnchor, GlobalPos> inRange, String outOfRange) {
        if (ctx.task().isPresent()) {
            return IntentCheck.fail("has_slip");
        }
        if (ctx.takeTaskCoolingDown()) {
            return IntentCheck.fail("board_resting");
        }
        return ctx.offeringBoard()
            .map(board -> inRange.test(ctx.anchor(), board) ? IntentCheck.OK : IntentCheck.fail(outOfRange))
            .orElseGet(() -> IntentCheck.fail("no_slip"));
    }
}
