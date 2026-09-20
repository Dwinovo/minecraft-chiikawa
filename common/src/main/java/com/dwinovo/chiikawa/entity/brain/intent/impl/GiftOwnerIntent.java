package com.dwinovo.chiikawa.entity.brain.intent.impl;

import com.dwinovo.chiikawa.entity.brain.intent.IntentCategory;
import com.dwinovo.chiikawa.entity.brain.intent.IntentCheck;
import com.dwinovo.chiikawa.entity.brain.intent.IntentContext;
import com.dwinovo.chiikawa.entity.brain.intent.PetIntent;
import com.dwinovo.chiikawa.entity.brain.intent.PetIntents;
import com.dwinovo.chiikawa.init.InitActivity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.schedule.Activity;

/**
 * Takes what the pet bought for its owner over to them.
 *
 * <p>Scored above everything a pet does for itself. A gift is the one errand where the
 * pet wants to be seen doing it, and a cake handed over three chores later is not the
 * same cake — an owner has to be able to connect the present to the pet standing in front
 * of them holding it.
 */
public final class GiftOwnerIntent implements PetIntent {
    private static final float SCORE = 0.85F;

    @Override
    public ResourceLocation id() {
        return PetIntents.GIFT_OWNER;
    }

    @Override
    public IntentCategory category() {
        return IntentCategory.GIFT;
    }

    @Override
    public Activity activity() {
        return InitActivity.GIFT_OWNER.get();
    }

    @Override
    public IntentCheck canRun(IntentContext ctx) {
        return ctx.carryingGift() ? IntentCheck.OK : IntentCheck.fail("no_gift");
    }

    @Override
    public IntentCheck canContinue(IntentContext ctx) {
        return canRun(ctx);
    }

    @Override
    public float score(IntentContext ctx) {
        return SCORE;
    }
}
