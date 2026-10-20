package com.dwinovo.chiikawa.entity.brain.errand.impl;

import com.dwinovo.chiikawa.entity.brain.errand.Check;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandTunings;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.init.InitActivity;
import java.util.Optional;
import net.minecraft.core.GlobalPos;

/**
 * Walks back to the owner. Only permitted while following, and only runs while the
 * anchor follows the owner (owner online, pet not leashed); otherwise the pet just
 * wanders.
 *
 * <p>Starts at the follow distance the anchor settings give and keeps going until the pet
 * has arrived, like 0.0.9. The score grows with distance, from the errand's own score up to
 * its {@code max_score}, so the further the owner gets the more it outweighs the other errands
 * of its priority.
 */
public final class FollowOwnerErrand extends LegacyErrand<Plan, ErrandTunings.FollowOwner> {
    /** The owner, who is the middle of the area it acts in, and so is always admitted. */
    private static final Heading TO_OWNER = new Heading(view ->
        view.anchor().followsOwner() ? Optional.of(view.anchor().center()) : Optional.empty());

    public FollowOwnerErrand() {
        super(ErrandIds.FOLLOW_OWNER, ErrandCategory.FOLLOW_OWNER, ErrandTunings.FollowOwner.CODEC,
            () -> InitActivity.FOLLOW_OWNER.get());
    }

    @Override
    protected Check canStart(PetView view, ErrandTunings.FollowOwner tuning) {
        if (!view.anchor().followsOwner()) {
            return Check.fail("owner_unavailable");
        }
        return distanceToOwner(view) >= view.follow().start() ? Check.OK : Check.fail("owner_nearby");
    }

    @Override
    protected Plan plan(PetView view, ErrandTunings.FollowOwner tuning) {
        return TO_OWNER;
    }

    @Override
    public Check stillWanted(Plan plan, PetView view, ErrandTunings.FollowOwner tuning) {
        if (!view.anchor().followsOwner()) {
            return Check.fail("owner_unavailable");
        }
        return view.pos().pos().distManhattan(view.anchor().center().pos()) > view.follow().arrive()
            ? Check.OK
            : Check.fail("owner_reached");
    }

    @Override
    public float score(Plan plan, PetView view, ErrandSettings<ErrandTunings.FollowOwner> settings) {
        double progress = (distanceToOwner(view) - view.follow().start()) / (view.follow().reach() - view.follow().start());
        float max = settings.tuning().maxScore();
        return settings.score() + (max - settings.score()) * (float) Math.max(0.0, Math.min(1.0, progress));
    }

    private static double distanceToOwner(PetView view) {
        return Math.sqrt(view.pos().pos().distSqr(view.anchor().center().pos()));
    }
}
