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
 * Sitting where it was told to, looking around: the habits of a wandering pet except the ones that move it. The only
 * errand a pet told to sit may be on. The sitting itself is the directive's, which the animation follows, so nothing
 * the errand does between one habit and the next, or when it is ended, stands the pet up.
 */
public final class StayErrand implements ErrandType<Plan, ErrandTunings.Untuned> {
    @Override
    public ResourceLocation id() {
        return ErrandIds.STAY;
    }

    @Override
    public ErrandCategory category() {
        return ErrandCategory.STAY;
    }

    @Override
    public Codec<ErrandTunings.Untuned> tuningCodec() {
        return ErrandTunings.Untuned.CODEC;
    }

    @Override
    public Offers<Plan> offers(PetView view, ErrandTunings.Untuned tuning) {
        return Habits.offers(view, false);
    }

    @Override
    public Routine routine(Plan plan, ErrandSettings<ErrandTunings.Untuned> settings) {
        return Habits.sitting(plan);
    }

    @Override
    public RestStore.Kind restStore() {
        return RestStore.shared();
    }
}
