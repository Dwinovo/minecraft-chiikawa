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
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.schedule.Activity;

/**
 * Goes over to another pet and plays a little scene with it, one the pet has thought of on
 * looking around (see {@code PetSocialSensor}).
 *
 * <p>Scored above pottering about and below any of the pet's errands and work, so a scene
 * fills idle time and never gets in the way of a job. Like the labor board, it starts only
 * on a partner within the anchor's reach and keeps going while the partner stays within
 * the leash: a pet at heel only plays with pets around its owner's feet.
 */
public final class SocializeIntent implements PetIntent {
    private static final float SCORE = 0.35F;

    @Override
    public Identifier id() {
        return PetIntents.SOCIALIZE;
    }

    @Override
    public IntentCategory category() {
        return IntentCategory.SOCIAL;
    }

    @Override
    public Activity activity() {
        return InitActivity.SOCIALIZE.get();
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

    private static IntentCheck check(IntentContext ctx, BiPredicate<PetAnchor, GlobalPos> inRange, String outOfRange) {
        return ctx.socialPartner()
            .map(partner -> inRange.test(ctx.anchor(), partner) ? IntentCheck.OK : IntentCheck.fail(outOfRange))
            .orElseGet(() -> IntentCheck.fail("no_partner"));
    }
}
