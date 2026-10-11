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
 * Walks to the nearest shop and spends some of what the pet has earned on something it
 * likes.
 *
 * <p>Scored above a day's work and below going for a slip, which is what makes the loop a
 * loop: a pet that has just been paid goes and spends a little before starting the next
 * job, and one with nothing in its pocket goes back to work without ever setting off.
 *
 * <p>The shop is where it goes, so the selector starts it only on a shop within the anchor's
 * reach and keeps it going while the shop stays within the leash: an owner who told a pet to
 * keep to the garden does not find it in town.
 */
public final class ShopIntent implements PetIntent {
    private static final float SCORE = 0.65F;

    @Override
    public ResourceLocation id() {
        return PetIntents.SHOP;
    }

    @Override
    public IntentCategory category() {
        return IntentCategory.SHOP;
    }

    @Override
    public Activity activity() {
        return InitActivity.SHOP.get();
    }

    @Override
    public IntentCheck canRun(IntentContext ctx) {
        if (ctx.shopCoolingDown()) {
            return IntentCheck.fail("shop_resting");
        }
        return ctx.shopWorthVisiting().isPresent() ? IntentCheck.OK : IntentCheck.fail("nothing_to_buy");
    }

    @Override
    public Optional<GlobalPos> destination(IntentContext ctx) {
        return ctx.shopWorthVisiting();
    }

    @Override
    public float score(IntentContext ctx) {
        return SCORE;
    }
}
