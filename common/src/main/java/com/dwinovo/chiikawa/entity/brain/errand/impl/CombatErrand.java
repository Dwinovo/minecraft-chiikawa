package com.dwinovo.chiikawa.entity.brain.errand.impl;

import com.dwinovo.chiikawa.entity.brain.errand.Check;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.schedule.Activity;

/**
 * Fights the remembered attack target, in melee or with a bow.
 *
 * <p>The target is where it goes: a fight starts only on a target within reach and off attack
 * cooldown, and keeps going between swings or shots while the target stays within the leash.
 * Past the leash the fight ends and the leash walks the pet back; since a new fight needs the
 * target back within reach, the pet does not flip between chasing and returning.
 *
 * @param <T> the shape of its tuning
 */
public final class CombatErrand<T> extends LegacyErrand<Plan, T> {
    private static final Heading TO_TARGET = new Heading(view -> view.targets().attackTarget());

    private final boolean needsArrows;

    public CombatErrand(ResourceLocation id, Codec<T> tuning, Supplier<Activity> activity, boolean needsArrows) {
        super(id, ErrandCategory.COMBAT, tuning, activity);
        this.needsArrows = needsArrows;
    }

    @Override
    protected Check canStart(PetView view, T tuning) {
        Check armed = armed(view);
        if (!armed.ok()) {
            return armed;
        }
        return view.attackCoolingDown() ? Check.fail("attack_cooling_down") : Check.OK;
    }

    @Override
    protected Plan plan(PetView view, T tuning) {
        return TO_TARGET;
    }

    /** Between swings the pet goes on, whatever its attack cooldown. */
    @Override
    public Check stillWanted(Plan plan, PetView view, T tuning) {
        return armed(view);
    }

    private Check armed(PetView view) {
        if (view.targets().attackTarget().isEmpty()) {
            return Check.fail("no_attack_target");
        }
        if (needsArrows && !view.hasArrows()) {
            return Check.fail("no_arrows");
        }
        return Check.OK;
    }
}
