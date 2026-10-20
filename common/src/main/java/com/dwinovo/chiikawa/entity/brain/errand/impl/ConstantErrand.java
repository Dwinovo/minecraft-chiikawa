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
 * An errand that can always start, such as staying put or wandering; whether it applies is left
 * entirely to the permission table.
 *
 * @param <T> the shape of its tuning
 */
public final class ConstantErrand<T> extends LegacyErrand<Plan, T> {
    public ConstantErrand(ResourceLocation id, ErrandCategory category, Codec<T> tuning, Supplier<Activity> activity) {
        super(id, category, tuning, activity);
    }

    @Override
    protected Check canStart(PetView view, T tuning) {
        return Check.OK;
    }

    @Override
    protected Plan plan(PetView view, T tuning) {
        return Heading.NOWHERE;
    }
}
