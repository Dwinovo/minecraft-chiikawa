package com.dwinovo.chiikawa.entity.brain.errand.work.action;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.brain.errand.Check;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandTunings;
import com.dwinovo.chiikawa.entity.brain.errand.Offer;
import com.dwinovo.chiikawa.entity.brain.errand.Offers;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.pastime.Destination;
import com.dwinovo.chiikawa.entity.brain.errand.pastime.Pastime;
import com.dwinovo.chiikawa.entity.brain.errand.pastime.PastimeTuning;
import com.dwinovo.chiikawa.entity.brain.errand.pastime.Stage;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestSources;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestStore;
import com.dwinovo.chiikawa.entity.brain.errand.run.Routine;
import com.dwinovo.chiikawa.entity.brain.errand.step.Goal;
import com.dwinovo.chiikawa.entity.brain.errand.step.Perform;
import com.dwinovo.chiikawa.entity.brain.errand.step.WalkTo;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.entity.brain.errand.view.Surroundings;
import com.dwinovo.chiikawa.entity.brain.errand.work.Work;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkAction;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkTarget;
import com.mojang.serialization.Codec;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Walks somewhere to pass the time, and passes it there, as its character does: a pastime whose file says when, and for
 * each character who does it where it goes, to its owner, to a creature it sees, to something lying about or out of the
 * rain, and what it does when it gets there, see {@link Pastime} and {@link Stage}. The place is found when the pet sets off,
 * from what the pet sees, and the pet gives the pastime up if it is gone before the pet is there or while it is there.
 */
public final class VisitAction implements WorkAction<VisitAction.Visiting, ErrandTunings.Visit> {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "visit");

    /**
     * What the pet has set out to do.
     *
     * @param errand the pastime, whose cooldown the rest after it is
     * @param stage what the pet's character does when it gets there
     * @param goal where it goes: its owner, a creature, or a place
     */
    public record Visiting(ResourceLocation errand, Stage stage, Goal goal) implements Plan {
        @Override
        public Optional<GlobalPos> destination(PetView view) {
            return switch (goal) {
                case Goal.Owner owner -> view.ownerPos();
                case Goal.Creature creature -> view.whereIs(creature.id());
                case Goal.Place place -> Optional.of(GlobalPos.of(view.pos().dimension(), place.pos()));
                default -> Optional.empty();
            };
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
    public Codec<ErrandTunings.Visit> tuningCodec() {
        return ErrandTunings.Visit.CODEC;
    }

    @Override
    public Optional<String> refuses(Optional<WorkTarget> target) {
        return target.map(ignored -> "visit takes no target");
    }

    @Override
    public Optional<String> refusesPastime(Optional<Pastime> pastime) {
        if (pastime.isEmpty()) {
            return Optional.of("visit needs a cast");
        }
        return pastime.get().cast().values().stream().anyMatch(stage -> stage.to().isEmpty())
            ? Optional.of("visit goes somewhere, and a stage of its cast does not say where") : Optional.empty();
    }

    @Override
    public Offers<Visiting> offers(Work<Visiting, ErrandTunings.Visit> work, ResourceLocation errand, PetView view,
            ErrandTunings.Visit tuning) {
        Optional<Stage> stage = work.pastime().orElseThrow().stageFor(view);
        if (stage.isEmpty()) {
            return Offers.none(Check.fail("no_occasion"));
        }
        Destination to = stage.get().to().orElseThrow();
        Optional<Goal> goal = aim(to, view);
        if (goal.isEmpty()) {
            return Offers.none(Check.fail(missing(to)));
        }
        return Offers.of(Offer.of(new Visiting(errand, stage.get(), goal.get())));
    }

    /** The place or the one it is for is still there. */
    @Override
    public Check stillWanted(Work<Visiting, ErrandTunings.Visit> work, ResourceLocation errand, Visiting plan, PetView view,
            ErrandTunings.Visit tuning) {
        return switch (plan.goal()) {
            case Goal.Owner owner -> view.ownerPos().isPresent() ? Check.OK : Check.fail("owner_unavailable");
            case Goal.Creature creature -> view.whereIs(creature.id()).isPresent() ? Check.OK : Check.fail("no_creature");
            default -> Check.OK;
        };
    }

    /**
     * @param to what the stage says to go to
     * @param view the pet
     * @return where that is now, as the pet sees it; nothing if there is none
     */
    private static Optional<Goal> aim(Destination to, PetView view) {
        Surroundings around = view.surroundings();
        return switch (to) {
            case Destination.ToOwner owner -> view.ownerPos().map(ignored -> new Goal.Owner());
            case Destination.ToShelter shelter -> around.shelter().map(Goal.Place::new);
            case Destination.ToCreature creature -> around.creatures().stream().filter(seen -> seen.kind().is(creature.kinds()))
                .findFirst().map(seen -> new Goal.Creature(seen.id()));
            case Destination.ToItem item -> around.items().stream()
                .filter(seen -> seen.kind().builtInRegistryHolder().is(item.kinds())).findFirst()
                .map(seen -> new Goal.Place(seen.where()));
        };
    }

    /** @return why there is nowhere to go, as the pet's reasons are worded */
    private static String missing(Destination to) {
        return switch (to) {
            case Destination.ToOwner owner -> "owner_unavailable";
            case Destination.ToShelter shelter -> "no_shelter";
            case Destination.ToCreature creature -> "no_creature";
            case Destination.ToItem item -> "no_item";
        };
    }

    @Override
    public Routine routine(Work<Visiting, ErrandTunings.Visit> work, Visiting plan, ErrandSettings<ErrandTunings.Visit> settings) {
        ErrandTunings.Visit tuning = settings.tuning();
        // What it came to see is what it looks at when it gets there; a place has nothing to look at, and is walked to, to the
        // very block, where its owner or a creature is only come up to.
        boolean toAPlace = plan.goal() instanceof Goal.Place;
        Optional<Goal> gaze = toAPlace ? Optional.empty() : Optional.of(plan.goal());
        return Routine.of(
            Routine.step(() -> WalkTo.near(plan.goal(), tuning.speed(), toAPlace ? 0 : tuning.arriveDistance(),
                toAPlace ? tuning.placeReach() : tuning.reach(), settings.maxTicks())),
            Routine.step(() -> new Perform(plan.stage(), gaze)));
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
