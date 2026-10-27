package com.dwinovo.chiikawa.entity.brain.errand.work.action;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.brain.errand.Check;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandTunings;
import com.dwinovo.chiikawa.entity.brain.errand.Offer;
import com.dwinovo.chiikawa.entity.brain.errand.Offers;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.pastime.Pastime;
import com.dwinovo.chiikawa.entity.brain.errand.pastime.PastimeTuning;
import com.dwinovo.chiikawa.entity.brain.errand.pastime.Stage;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestSources;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestStore;
import com.dwinovo.chiikawa.entity.brain.errand.run.Routine;
import com.dwinovo.chiikawa.entity.brain.errand.step.Perform;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.entity.brain.errand.work.Work;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkAction;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkTarget;
import com.mojang.serialization.Codec;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * Passes the time where the pet stands, as its character does: a pastime whose file says when, and for each character who
 * does it what it does, see {@link Pastime} and {@link Stage}. Offered to a pet that has a part in it while everything the
 * file says has to be so is, and gone on with for as long as the stage lasts. The pet rests from it for the cooldown of
 * its tuning, divided by the world's setting for how often pets pass the time, see {@link PastimeTuning}.
 */
public final class PerformAction implements WorkAction<PerformAction.Performing, ErrandTunings.Perform> {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "perform");

    /**
     * What the pet has set out to do.
     *
     * @param errand the pastime, whose cooldown the rest after it is
     * @param stage what the pet's character does
     */
    public record Performing(ResourceLocation errand, Stage stage) implements Plan {
        @Override
        public Optional<ResourceLocation> restKey() {
            return Optional.of(errand);
        }
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public Codec<ErrandTunings.Perform> tuningCodec() {
        return ErrandTunings.Perform.CODEC;
    }

    @Override
    public Optional<String> refuses(Optional<WorkTarget> target) {
        return target.map(ignored -> "perform takes no target");
    }

    @Override
    public Optional<String> refusesPastime(Optional<Pastime> pastime) {
        if (pastime.isEmpty()) {
            return Optional.of("perform needs a cast");
        }
        return pastime.get().cast().values().stream().anyMatch(stage -> stage.to().isPresent())
            ? Optional.of("perform stays where it is, and a stage of its cast goes somewhere") : Optional.empty();
    }

    @Override
    public Offers<Performing> offers(Work<Performing, ErrandTunings.Perform> work, ResourceLocation errand, PetView view,
            ErrandTunings.Perform tuning) {
        return work.pastime().orElseThrow().stageFor(view)
            .map(stage -> Offers.of(Offer.of(new Performing(errand, stage))))
            .orElseGet(() -> Offers.none(Check.fail("no_occasion")));
    }

    @Override
    public Routine routine(Work<Performing, ErrandTunings.Perform> work, Performing plan,
            ErrandSettings<ErrandTunings.Perform> settings) {
        return Routine.single(new Perform(plan.stage(), Optional.empty()));
    }

    @Override
    public RestStore.Kind restStore() {
        return RestStore.shared();
    }

    @Override
    public Map<ResourceLocation, RestSources.Resolver> restSources() {
        return Map.of(RestSources.PASTIME_COOLDOWN, PastimeTuning.COOLDOWN);
    }
}
