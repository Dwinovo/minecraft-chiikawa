package com.dwinovo.chiikawa.entity.brain.errand.decide;

import com.dwinovo.chiikawa.entity.brain.errand.Check;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandType;
import com.dwinovo.chiikawa.entity.brain.errand.Offer;
import com.dwinovo.chiikawa.entity.brain.errand.Offers;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestLedger;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.task.PetTask;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/**
 * An errand the pet could start now, with the offers that got through: the filter every errand is
 * put through before it competes, and the score it competes with.
 *
 * <p>In order: the directive permits its category, it is not resting, it has something to offer, and
 * what it offers is within the reach of the area the pet acts in. The rule about where a pet goes is
 * asked here and only here, so no errand checks the area itself.
 *
 * @param <P> the shape of the errand's plans
 * @param <T> the shape of its tuning
 */
record Admission<P extends Plan, T>(ErrandType<P, T> type, ErrandSettings<T> settings, List<Offer<P>> offers) {
    /**
     * @param type the errand
     * @param settings its settings, which are those of the same errand
     * @param trace where to note why the errand is out, if somebody is looking
     * @return the errand with its offers, or nothing if it could not start
     */
    @SuppressWarnings("unchecked")
    static <P extends Plan, T> Optional<Admission<P, T>> of(ErrandType<P, T> type, ErrandSettings<?> settings,
            PetView view, Permits permits, RestLedger rests, long now, @Nullable DecisionTrace trace) {
        int priority = settings.priority();
        if (!permits.allows(type.category())) {
            return out(trace, type, priority, DecisionTrace.NOT_PERMITTED);
        }
        if (rests.resting(type.id(), now)) {
            return out(trace, type, priority, DecisionTrace.RESTING);
        }
        T tuning = (T) settings.tuning();
        Offers<P> offers = type.offers(view, tuning);
        if (offers.isEmpty()) {
            return out(trace, type, priority, offers.check().reasonKey());
        }
        List<Offer<P>> unrested = offers.list().stream().filter(offer -> offer.plan().subject()
            .filter(subject -> rests.resting(type.id(), subject, now)).isEmpty()).toList();
        if (unrested.isEmpty()) {
            return out(trace, type, priority, DecisionTrace.RESTING);
        }
        List<Offer<P>> reachable = unrested.stream().filter(offer -> offer.plan().destination(view)
            .filter(destination -> !view.anchor().admits(destination, true)).isEmpty()).toList();
        if (reachable.isEmpty()) {
            return out(trace, type, priority, Check.fail("out_of_reach").reasonKey());
        }
        return Optional.of(new Admission<>(type, (ErrandSettings<T>) settings, reachable));
    }

    private static <P extends Plan, T> Optional<Admission<P, T>> out(@Nullable DecisionTrace trace, ErrandType<P, T> type,
            int priority, String reason) {
        if (trace != null) {
            trace.excluded(type.id(), priority, reason);
        }
        return Optional.empty();
    }

    /**
     * @return what the pet makes of the errand: its score, raised by {@link Decision#TASK_BONUS} when the pet's slip
     *         counts its work and by {@link Decision#EAGER_BONUS} when it is work and the pet is keen, weighted by the
     *         pet's personality for this part of the day
     */
    Scored scored(PetView view) {
        boolean carriesSlip = type.workCounter().isPresent() && view.slip().map(PetTask::counter).equals(type.workCounter());
        boolean keen = view.eager()
            && (type.category() == ErrandCategory.WORK || type.category() == ErrandCategory.FORAGE);
        float score = (type.score(offers.get(0).plan(), view, settings)
            + (carriesSlip ? Decision.TASK_BONUS : 0.0F)
            + (keen ? Decision.EAGER_BONUS : 0.0F))
            * view.personality().multiplier(type.id(), view.phase());
        return new Scored(type.id(), settings.priority(), score, offers.stream().map(Offer::weight).toList());
    }
}
