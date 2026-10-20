package com.dwinovo.chiikawa.entity.brain.errand.impl;

import com.dwinovo.chiikawa.entity.brain.errand.Check;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandType;
import com.dwinovo.chiikawa.entity.brain.errand.Offer;
import com.dwinovo.chiikawa.entity.brain.errand.Offers;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestStore;
import com.dwinovo.chiikawa.entity.brain.errand.run.Routine;
import com.dwinovo.chiikawa.entity.brain.errand.step.LegacyActivityStep;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.schedule.Activity;

/**
 * Scaffolding, deleted in T17: an errand that is still carried out by one old brain activity. What it
 * starts on is what the intent it came from started on, as a condition that gives one offer; what it
 * does is one {@link LegacyActivityStep}. Each errand leaves this for a routine of its own as it goes native.
 *
 * @param <P> the shape of its plans
 * @param <T> the shape of its tuning
 */
public abstract class LegacyErrand<P extends Plan, T> implements ErrandType<P, T> {
    private final ResourceLocation id;
    private final ErrandCategory category;
    private final Codec<T> tuning;
    private final Supplier<Activity> activity;

    protected LegacyErrand(ResourceLocation id, ErrandCategory category, Codec<T> tuning, Supplier<Activity> activity) {
        this.id = id;
        this.category = category;
        this.tuning = tuning;
        this.activity = activity;
    }

    /** @return whether the pet could set out on the errand now, and if not why not; pure */
    protected abstract Check canStart(PetView view, T tuning);

    /** @return what the pet sets out to do, once it can; pure */
    protected abstract P plan(PetView view, T tuning);

    /** Goes on while it could start, as the intents it came from did, unless it says otherwise. */
    @Override
    public Check stillWanted(P plan, PetView view, T tuning) {
        return canStart(view, tuning);
    }

    /** Called as the errand starts, after its activity is entered: for an errand whose behavior carries out a choice made then. */
    protected void onStart(P plan, LegacyActivityStep.Runtime runtime) {
    }

    /** Called as the errand ends, before its activity is left and its behaviors are stopped. */
    protected void onStop(LegacyActivityStep.Runtime runtime) {
    }

    @Override
    public final ResourceLocation id() {
        return id;
    }

    @Override
    public final ErrandCategory category() {
        return category;
    }

    @Override
    public final Codec<T> tuningCodec() {
        return tuning;
    }

    @Override
    public final Offers<P> offers(PetView view, T tuning) {
        Check start = canStart(view, tuning);
        return start.ok() ? Offers.of(Offer.of(plan(view, tuning))) : Offers.none(start);
    }

    @Override
    public final Routine routine(P plan, ErrandSettings<T> settings) {
        return Routine.single(new LegacyActivityStep(activity.get(), runtime -> onStart(plan, runtime), this::onStop));
    }

    /** Its behaviors keep its rest, in the memories they always have. */
    @Override
    public final RestStore.Kind restStore() {
        return RestStore.legacy();
    }
}
