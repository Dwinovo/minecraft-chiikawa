package com.dwinovo.chiikawa.entity.brain.errand.impl;

import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandTunings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandType;
import com.dwinovo.chiikawa.entity.brain.errand.Offers;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestStore;
import com.dwinovo.chiikawa.entity.brain.errand.run.Routine;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;

/**
 * Pottering about with nothing to do: looking at whoever is near, strolling a few steps, doing nothing in
 * particular, and now and then saying something (the {@code idle_chatter} overlay of its settings). Each habit is a
 * short errand of its own, see {@link Habits}, and the pet picks again between them. A pet that has strayed past the
 * leash of its area offers nothing but the way back.
 */
public final class WanderErrand implements ErrandType<Plan, ErrandTunings.Wander> {
    @Override
    public ResourceLocation id() {
        return ErrandIds.WANDER;
    }

    @Override
    public ErrandCategory category() {
        return ErrandCategory.WANDER;
    }

    @Override
    public Codec<ErrandTunings.Wander> tuningCodec() {
        return ErrandTunings.Wander.CODEC;
    }

    @Override
    public Offers<Plan> offers(PetView view, ErrandTunings.Wander tuning) {
        return Habits.offers(view, true);
    }

    @Override
    public Routine routine(Plan plan, ErrandSettings<ErrandTunings.Wander> settings) {
        return Habits.wandering(plan, settings.tuning(), settings.maxTicks());
    }

    @Override
    public RestStore.Kind restStore() {
        return RestStore.shared();
    }
}
