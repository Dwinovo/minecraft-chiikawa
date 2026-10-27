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
import com.dwinovo.chiikawa.entity.brain.errand.step.Ask;
import com.dwinovo.chiikawa.entity.brain.errand.step.Goal;
import com.dwinovo.chiikawa.entity.brain.errand.step.Perform;
import com.dwinovo.chiikawa.entity.brain.errand.step.WalkTo;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.entity.brain.errand.work.Work;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkAction;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkTarget;
import com.mojang.serialization.Codec;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Asks the owner to say how good the pet is, as Momonga does: a pastime whose file says when and what the pet's character
 * does while it waits for the praise and as the waiting runs out, see {@link Pastime} and {@link Stage}. The pet first copies
 * what the pet nearest it is doing, if it has the animation for it, and goes to its owner, and asks. A click from its owner or
 * a friend of theirs is the praise, see {@code PetInteractHandler}, and then it is glad; if nobody gives it, it turns to a
 * tantrum and is done. It rests for the cooldown of its tuning, divided by the world's setting for how often pets pass the
 * time, see {@link PastimeTuning}.
 */
public final class AskPraiseAction implements WorkAction<AskPraiseAction.Asking, ErrandTunings.AskPraise> {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "ask_praise");

    /**
     * What the pet has set out to do.
     *
     * @param errand the pastime, whose cooldown the rest after it is
     * @param stage what the pet's character does while it asks
     * @param imitate the animations it copies on the way, the nearest pet's first and then what it does for want of one
     */
    public record Asking(ResourceLocation errand, Stage stage, String imitate) implements Plan {
        @Override
        public Optional<GlobalPos> destination(PetView view) {
            return view.ownerPos();
        }

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
    public Codec<ErrandTunings.AskPraise> tuningCodec() {
        return ErrandTunings.AskPraise.CODEC;
    }

    @Override
    public Optional<String> refuses(Optional<WorkTarget> target) {
        return target.map(ignored -> "ask_praise takes no target");
    }

    @Override
    public Optional<String> refusesPastime(Optional<Pastime> pastime) {
        if (pastime.isEmpty()) {
            return Optional.of("ask_praise needs a cast");
        }
        return pastime.get().cast().values().stream().anyMatch(stage -> stage.to().isPresent())
            ? Optional.of("ask_praise goes to its owner, and a stage of its cast names somewhere else") : Optional.empty();
    }

    @Override
    public Offers<Asking> offers(Work<Asking, ErrandTunings.AskPraise> work, ResourceLocation errand, PetView view,
            ErrandTunings.AskPraise tuning) {
        Optional<Stage> stage = work.pastime().orElseThrow().stageFor(view);
        if (stage.isEmpty()) {
            return Offers.none(Check.fail("no_occasion"));
        }
        if (view.ownerPos().isEmpty()) {
            return Offers.none(Check.fail("owner_unavailable"));
        }
        String imitate = view.neighbourPerformance().map(copied -> copied + "," + tuning.imitates()).orElse(tuning.imitates());
        return Offers.of(Offer.of(new Asking(errand, stage.get(), imitate)));
    }

    /** The one it asks is still there to be asked. */
    @Override
    public Check stillWanted(Work<Asking, ErrandTunings.AskPraise> work, ResourceLocation errand, Asking plan, PetView view,
            ErrandTunings.AskPraise tuning) {
        return view.ownerPos().isPresent() ? Check.OK : Check.fail("owner_unavailable");
    }

    @Override
    public Routine routine(Work<Asking, ErrandTunings.AskPraise> work, Asking plan, ErrandSettings<ErrandTunings.AskPraise> settings) {
        ErrandTunings.AskPraise tuning = settings.tuning();
        Stage copying = new Stage(Optional.empty(), Optional.of(plan.imitate()), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), tuning.imitateTicks());
        return Routine.of(
            Routine.step(() -> new Perform(copying, Optional.empty())),
            Routine.step(() -> WalkTo.near(new Goal.Owner(), tuning.speed(), tuning.arriveDistance(), tuning.reach(), settings.maxTicks())),
            Routine.step(() -> new Ask(plan.stage(), new Goal.Owner())));
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
