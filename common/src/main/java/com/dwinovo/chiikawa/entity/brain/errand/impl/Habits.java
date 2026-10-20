package com.dwinovo.chiikawa.entity.brain.errand.impl;

import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.entity.brain.errand.Check;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandTunings;
import com.dwinovo.chiikawa.entity.brain.errand.Offer;
import com.dwinovo.chiikawa.entity.brain.errand.Offers;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.run.Routine;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.step.Amble;
import com.dwinovo.chiikawa.entity.brain.errand.step.Glance;
import com.dwinovo.chiikawa.entity.brain.errand.step.Pause;
import com.dwinovo.chiikawa.entity.brain.errand.view.Onlooker;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.entity.brain.personality.IdleHabits;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.GlobalPos;

/**
 * What a pet does with itself when it has nothing to do, shared by wandering and sitting: each of the pet's idle
 * habits is a way to go about the errand, offered while the pet has a reason to and weighed by the weight its
 * personality gives it, and each is one short step that ends when the habit is over, so the pet picks again between
 * habits. A sitting pet has the same habits except the ones that move it.
 */
final class Habits {
    private Habits() {
    }

    /**
     * Looking at someone who is near.
     *
     * @param kind whom
     * @param target who
     */
    record Looking(Onlooker kind, UUID target) implements Plan {
    }

    /** Doing nothing in particular. */
    record Resting() implements Plan {
    }

    /** A few steps somewhere, which the pet picks when it sets off. */
    record Strolling() implements Plan {
    }

    /**
     * Going back to the middle of the area the pet keeps to, because it has strayed past the leash.
     *
     * @param center the middle of the area
     */
    record Returning(GlobalPos center) implements Plan {
        @Override
        public Optional<GlobalPos> destination(PetView view) {
            return Optional.of(center);
        }
    }

    /**
     * @param view the pet
     * @param moving whether the pet may move itself, which is wandering and not sitting: it may stroll, and once it has
     *               strayed past its leash it does nothing but return
     * @return the habits it has a reason to do now, or why it has none
     */
    static Offers<Plan> offers(PetView view, boolean moving) {
        PetAnchor anchor = view.anchor();
        if (moving && anchor.canMove() && !anchor.withinLeash(view.pos())) {
            return Offers.of(Offer.of(new Returning(anchor.center())));
        }
        IdleHabits habits = view.personality().idle();
        List<Offer<Plan>> offers = new ArrayList<>();
        looking(offers, view, Onlooker.PLAYER, habits);
        looking(offers, view, Onlooker.CREATURE, habits);
        if (moving && habits.stroll() > 0) {
            offers.add(new Offer<>(new Strolling(), habits.stroll()));
        }
        if (habits.rest().weight() > 0) {
            offers.add(new Offer<>(new Resting(), habits.rest().weight()));
        }
        return offers.isEmpty() ? Offers.none(Check.fail("nothing_to_do")) : Offers.of(offers);
    }

    private static void looking(List<Offer<Plan>> offers, PetView view, Onlooker kind, IdleHabits habits) {
        IdleHabits.Glance habit = kind.habit(habits);
        if (habit.weight() > 0) {
            view.nearestVisible(kind, habit.range())
                .ifPresent(target -> offers.add(new Offer<>(new Looking(kind, target), habit.weight())));
        }
    }

    /** @return the routine of a habit of a pet that stays where it is: one step */
    static Routine sitting(Plan plan) {
        return Routine.single(stillStep(plan));
    }

    /**
     * @param wander how a wandering pet strolls and returns
     * @param maxTicks the longest the errand lasts, which is the longest the way back to the area is walked
     * @return the routine of a habit of a wandering pet: one step
     */
    static Routine wandering(Plan plan, ErrandTunings.Wander wander, int maxTicks) {
        Step step = switch (plan) {
            case Strolling strolling -> Amble.stroll(wander.strollSpeed(), wander.strollRadius(), wander.strollAttempts(),
                wander.strollTicks());
            case Returning returning -> Amble.homeward(returning.center().pos(), wander.returnSpeed(), maxTicks);
            default -> stillStep(plan);
        };
        return Routine.single(step);
    }

    private static Step stillStep(Plan plan) {
        return switch (plan) {
            case Looking looking -> new Glance(looking.kind(), looking.target());
            case Resting resting -> new Pause();
            default -> throw new IllegalArgumentException(plan + " is not a habit of a pet that stays where it is");
        };
    }
}
