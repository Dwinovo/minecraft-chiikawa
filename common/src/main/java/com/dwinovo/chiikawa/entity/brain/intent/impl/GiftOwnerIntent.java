package com.dwinovo.chiikawa.entity.brain.intent.impl;

import com.dwinovo.chiikawa.entity.brain.intent.IntentCategory;
import com.dwinovo.chiikawa.entity.brain.intent.IntentCheck;
import com.dwinovo.chiikawa.entity.brain.intent.IntentContext;
import com.dwinovo.chiikawa.entity.brain.intent.PetIntent;
import com.dwinovo.chiikawa.entity.brain.intent.PetIntents;
import com.dwinovo.chiikawa.init.InitActivity;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.schedule.Activity;

/**
 * Takes the owner a present out of the pet's bag. It runs when the pet is carrying one already, or
 * has one in mind: its owner is about, its last present was long enough ago and there is something
 * in its bag it may give; picking the thing is the behavior's, as it sets off.
 *
 * <p>Scored above everything a pet does for itself. A gift is the one errand where the
 * pet wants to be seen doing it, and a cake handed over three chores later is not the
 * same cake — an owner has to be able to connect the present to the pet standing in front
 * of them holding it.
 *
 * <p>Only while the pet is carrying one does it go on: once the present is handed over, or put back
 * in the bag, the errand is done.
 */
public final class GiftOwnerIntent implements PetIntent {
    private static final float SCORE = 0.85F;

    @Override
    public Identifier id() {
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
        return ctx.carryingGift() || ctx.presentToGive() ? IntentCheck.OK : IntentCheck.fail("no_gift");
    }

    @Override
    public IntentCheck canContinue(IntentContext ctx) {
        return ctx.carryingGift() ? IntentCheck.OK : IntentCheck.fail("no_gift");
    }

    @Override
    public float score(IntentContext ctx) {
        return SCORE;
    }
}
