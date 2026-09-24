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
 * The other side of a scene: while another pet is on its way over, stays where it is,
 * turns to it and plays its part once it arrives.
 *
 * <p>A pet is only asked while it is pottering about, and asking it makes it re-decide at
 * once. Scored well clear of wandering, so whatever the pet's randomness it answers rather
 * than strolls off; still below work, so something that needs doing takes it away, and
 * the one coming over gives up.
 */
public final class CooperateIntent implements PetIntent {
    private static final float SCORE = 0.5F;

    @Override
    public Identifier id() {
        return PetIntents.COOPERATE;
    }

    @Override
    public IntentCategory category() {
        return IntentCategory.SOCIAL;
    }

    @Override
    public Activity activity() {
        return InitActivity.COOPERATE.get();
    }

    @Override
    public IntentCheck canRun(IntentContext ctx) {
        return ctx.playingAlong() ? IntentCheck.OK : IntentCheck.fail("not_asked");
    }

    @Override
    public float score(IntentContext ctx) {
        return SCORE;
    }
}
