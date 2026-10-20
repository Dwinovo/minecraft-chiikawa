package com.dwinovo.chiikawa.entity.brain.intent.impl;

import com.dwinovo.chiikawa.entity.brain.intent.IntentCategory;
import com.dwinovo.chiikawa.entity.brain.intent.IntentCheck;
import com.dwinovo.chiikawa.entity.brain.intent.IntentContext;
import com.dwinovo.chiikawa.entity.brain.intent.PetIntent;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.schedule.Activity;

/**
 * Fights the remembered attack target, in melee or with a bow.
 *
 * <p>The target is where it goes: a fight starts only on a target within reach and off attack
 * cooldown, and keeps going between swings or shots while the target stays within the leash.
 * Past the leash the fight ends and the leash walks the pet back; since a new fight needs the
 * target back within reach, the pet does not flip between chasing and returning.
 */
public final class CombatIntent implements PetIntent {
    private final ResourceLocation id;
    private final Supplier<Activity> activity;
    private final boolean needsArrows;

    public CombatIntent(ResourceLocation id, Supplier<Activity> activity, boolean needsArrows) {
        this.id = id;
        this.activity = activity;
        this.needsArrows = needsArrows;
    }

    @Override
    public ResourceLocation id() {
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
        return ctx.attackCoolingDown() ? IntentCheck.fail("attack_cooling_down") : IntentCheck.OK;
    }

    /** Between swings the pet goes on, whatever its attack cooldown. */
    @Override
    public IntentCheck canContinue(IntentContext ctx) {
        return checkArmed(ctx);
    }

    @Override
    public Optional<GlobalPos> destination(IntentContext ctx) {
        return ctx.targets().attackTarget();
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
