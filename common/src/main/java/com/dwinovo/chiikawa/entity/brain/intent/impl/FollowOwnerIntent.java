package com.dwinovo.chiikawa.entity.brain.intent.impl;

import com.dwinovo.chiikawa.entity.brain.constraint.AnchorDistances;
import com.dwinovo.chiikawa.entity.brain.intent.IntentCategory;
import com.dwinovo.chiikawa.entity.brain.intent.IntentCheck;
import com.dwinovo.chiikawa.entity.brain.intent.IntentContext;
import com.dwinovo.chiikawa.entity.brain.intent.PetIntent;
import com.dwinovo.chiikawa.entity.brain.intent.PetIntents;
import com.dwinovo.chiikawa.init.InitActivity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.schedule.Activity;

/**
 * Walks back to the owner. Only permitted while following, where the anchor is
 * centered on the owner whenever the pet can actually follow (owner online,
 * pet not leashed); otherwise the anchor has no teleport distance and this intent
 * never starts.
 *
 * <p>Starts at {@link AnchorDistances#FOLLOW_START} and keeps going until the pet
 * has arrived, like 0.0.9. The score grows with distance, so the further the owner
 * gets the more it outweighs anything done on the way.
 */
public final class FollowOwnerIntent implements PetIntent {
    private static final float MIN_SCORE = 0.5F;
    private static final float MAX_SCORE = 1.0F;

    @Override
    public ResourceLocation id() {
        return PetIntents.FOLLOW_OWNER;
    }

    @Override
    public IntentCategory category() {
        return IntentCategory.FOLLOW_OWNER;
    }

    @Override
    public Activity activity() {
        return InitActivity.FOLLOW_OWNER.get();
    }

    @Override
    public IntentCheck canRun(IntentContext ctx) {
        if (ctx.anchor().teleport().isEmpty()) {
            return IntentCheck.fail("owner_unavailable");
        }
        return distanceToOwner(ctx) >= AnchorDistances.FOLLOW_START ? IntentCheck.OK : IntentCheck.fail("owner_nearby");
    }

    @Override
    public IntentCheck canContinue(IntentContext ctx) {
        if (ctx.anchor().teleport().isEmpty()) {
            return IntentCheck.fail("owner_unavailable");
        }
        return ctx.petPos().pos().distManhattan(ctx.anchor().center().pos()) > AnchorDistances.FOLLOW_ARRIVE
            ? IntentCheck.OK
            : IntentCheck.fail("owner_reached");
    }

    @Override
    public float score(IntentContext ctx) {
        double progress = (distanceToOwner(ctx) - AnchorDistances.FOLLOW_START)
            / (AnchorDistances.FOLLOW_REACH - AnchorDistances.FOLLOW_START);
        return MIN_SCORE + (MAX_SCORE - MIN_SCORE) * (float) Math.max(0.0, Math.min(1.0, progress));
    }

    private static double distanceToOwner(IntentContext ctx) {
        return Math.sqrt(ctx.petPos().pos().distSqr(ctx.anchor().center().pos()));
    }
}
