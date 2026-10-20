package com.dwinovo.chiikawa.entity.brain.errand.impl;

import com.dwinovo.chiikawa.entity.brain.constraint.AnchorSettings;
import com.dwinovo.chiikawa.entity.brain.errand.Check;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandTunings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandType;
import com.dwinovo.chiikawa.entity.brain.errand.Offer;
import com.dwinovo.chiikawa.entity.brain.errand.Offers;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestStore;
import com.dwinovo.chiikawa.entity.brain.errand.run.Routine;
import com.dwinovo.chiikawa.entity.brain.errand.step.FollowEntity;
import com.dwinovo.chiikawa.entity.brain.errand.step.Goal;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Walks back to the owner and stays at their side. Only permitted while following, and only runs while the
 * anchor follows the owner (owner online, pet not leashed or riding); otherwise the pet just wanders.
 *
 * <p>Starts at the follow distance the anchor settings give and is done once the pet has arrived at the arrival
 * distance they give, like 0.0.9: the anchor owns both and the errand reads them. The score grows with distance, from
 * the errand's own score up to its {@code max_score}, so the further the owner gets the more it outweighs the other
 * errands of its priority.
 *
 * <p>Fetching a pet that has fallen too far behind is not this errand's: the server does it, see
 * {@link com.dwinovo.chiikawa.entity.PetFollowKeeper}, since a pet that has stopped thinking cannot fetch itself.
 */
public final class FollowOwnerErrand implements ErrandType<FollowOwnerErrand.Heel, ErrandTunings.FollowOwner> {
    /** Going to the owner, who is the middle of the area the pet acts in, and so is always within it. */
    public record Heel() implements Plan {
        @Override
        public Optional<GlobalPos> destination(PetView view) {
            return view.anchor().followsOwner() ? Optional.of(view.anchor().center()) : Optional.empty();
        }
    }

    @Override
    public ResourceLocation id() {
        return ErrandIds.FOLLOW_OWNER;
    }

    @Override
    public ErrandCategory category() {
        return ErrandCategory.FOLLOW_OWNER;
    }

    @Override
    public Codec<ErrandTunings.FollowOwner> tuningCodec() {
        return ErrandTunings.FollowOwner.CODEC;
    }

    @Override
    public Offers<Heel> offers(PetView view, ErrandTunings.FollowOwner tuning) {
        if (!view.anchor().followsOwner()) {
            return Offers.none(Check.fail("owner_unavailable"));
        }
        return distanceToOwner(view) >= view.follow().start()
            ? Offers.of(Offer.of(new Heel()))
            : Offers.none(Check.fail("owner_nearby"));
    }

    /** Arriving is the step's end, so the errand is not given up on at the very moment the pet gets there. */
    @Override
    public Check stillWanted(Heel plan, PetView view, ErrandTunings.FollowOwner tuning) {
        return view.anchor().followsOwner() ? Check.OK : Check.fail("owner_unavailable");
    }

    @Override
    public float score(Heel plan, PetView view, ErrandSettings<ErrandTunings.FollowOwner> settings) {
        double progress = (distanceToOwner(view) - view.follow().start()) / (view.follow().reach() - view.follow().start());
        float max = settings.tuning().maxScore();
        return settings.score() + (max - settings.score()) * (float) Math.max(0.0, Math.min(1.0, progress));
    }

    @Override
    public Routine routine(Heel plan, ErrandSettings<ErrandTunings.FollowOwner> settings) {
        return Routine.of(Routine.step(() -> new FollowEntity(new Goal.Owner(), settings.tuning().speed(),
            () -> AnchorSettings.current().map(anchor -> anchor.follow().arrive()).orElse(0))));
    }

    @Override
    public RestStore.Kind restStore() {
        return RestStore.shared();
    }

    private static double distanceToOwner(PetView view) {
        return Math.sqrt(view.pos().pos().distSqr(view.anchor().center().pos()));
    }
}
