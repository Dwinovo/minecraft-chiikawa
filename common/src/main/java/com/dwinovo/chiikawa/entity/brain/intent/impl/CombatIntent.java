package com.dwinovo.chiikawa.entity.brain.intent.impl;

import com.dwinovo.chiikawa.entity.brain.intent.IntentCategory;
import com.dwinovo.chiikawa.entity.brain.intent.IntentCheck;
import com.dwinovo.chiikawa.entity.brain.intent.IntentContext;
import com.dwinovo.chiikawa.entity.brain.intent.PetIntent;
import java.util.function.Supplier;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.schedule.Activity;

/**
 * Fights the remembered attack target, in melee or with a bow.
 *
 * <p>A fight starts only on a target within reach and off attack cooldown, and keeps
 * going between swings or shots while the target stays within the leash. Past the
 * leash the fight ends and the leash walks the pet back; since a new fight needs the
 * target back within reach, the pet does not flip between chasing and returning.
 */
public final class CombatIntent implements PetIntent {
    private static final float SCORE = 0.8F;

    private final Identifier id;
    private final Supplier<Activity> activity;
    private final boolean needsArrows;

    public CombatIntent(Identifier id, Supplier<Activity> activity, boolean needsArrows) {
        this.id = id;
        this.activity = activity;
        this.needsArrows = needsArrows;
    }

    @Override
    public Identifier id() {
        return id;
    }

    @Override
    public IntentCategory category() {
        return IntentCategory.COMBAT;
    }

    @Override
    public Activity activity() {
        return activity.get();
    }

    @Override
    public IntentCheck canRun(IntentContext ctx) {
        IntentCheck armed = checkArmed(ctx);
        if (!armed.ok()) {
            return armed;
        }
        if (ctx.attackCoolingDown()) {
            return IntentCheck.fail("attack_cooling_down");
        }
        GlobalPos target = ctx.targets().attackTarget().get();
        return ctx.anchor().withinReach(target) ? IntentCheck.OK : IntentCheck.fail("out_of_reach");
    }

    @Override
    public IntentCheck canContinue(IntentContext ctx) {
        IntentCheck armed = checkArmed(ctx);
        if (!armed.ok()) {
            return armed;
        }
        GlobalPos target = ctx.targets().attackTarget().get();
        return ctx.anchor().withinLeash(target) ? IntentCheck.OK : IntentCheck.fail("out_of_leash");
    }

    @Override
    public float score(IntentContext ctx) {
        return SCORE;
    }

    private IntentCheck checkArmed(IntentContext ctx) {
        if (ctx.targets().attackTarget().isEmpty()) {
            return IntentCheck.fail("no_attack_target");
        }
        if (needsArrows && !ctx.hasArrows()) {
            return IntentCheck.fail("no_arrows");
        }
        return IntentCheck.OK;
    }
}
