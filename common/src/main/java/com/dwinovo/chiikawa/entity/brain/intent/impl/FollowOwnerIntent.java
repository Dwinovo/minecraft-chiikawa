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
 * Walks back to the owner. Only permitted while following, and only runs while the
 * anchor follows the owner (owner online, pet not leashed); otherwise the pet just
 * wanders.
 *
 * <p>Starts at the follow distance the anchor settings give and keeps going until the pet
 * has arrived, like 0.0.9. The score grows with distance, so the further the owner
 * gets the more it outweighs anything done on the way.
 */
public final class FollowOwnerIntent implements PetIntent {
    private static final float MIN_SCORE = 0.5F;
    private static final float MAX_SCORE = 1.0F;

    @Override
    public Identifier id() {
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
        if (!ctx.anchor().followsOwner()) {
            return IntentCheck.fail("owner_unavailable");
        }
        return distanceToOwner(ctx) >= ctx.follow().start() ? IntentCheck.OK : IntentCheck.fail("owner_nearby");
    }

    @Override
    public IntentCheck canContinue(IntentContext ctx) {
        if (!ctx.anchor().followsOwner()) {
            return IntentCheck.fail("owner_unavailable");
        }
        return ctx.petPos().pos().distManhattan(ctx.anchor().center().pos()) > ctx.follow().arrive()
            ? IntentCheck.OK
            : IntentCheck.fail("owner_reached");
    }

    /** The owner, who is the middle of the area it acts in, and so is always admitted. */
    @Override
    public Optional<GlobalPos> destination(IntentContext ctx) {
        return ctx.anchor().followsOwner() ? Optional.of(ctx.anchor().center()) : Optional.empty();
    }

    @Override
    public float score(IntentContext ctx) {
        double progress = (distanceToOwner(ctx) - ctx.follow().start()) / (ctx.follow().reach() - ctx.follow().start());
        return MIN_SCORE + (MAX_SCORE - MIN_SCORE) * (float) Math.max(0.0, Math.min(1.0, progress));
    }

    private static double distanceToOwner(IntentContext ctx) {
        return Math.sqrt(ctx.petPos().pos().distSqr(ctx.anchor().center().pos()));
    }
}
