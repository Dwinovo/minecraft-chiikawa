package com.dwinovo.chiikawa.entity.brain.errand.decide;

import com.dwinovo.chiikawa.entity.brain.errand.Check;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandType;
import com.dwinovo.chiikawa.entity.brain.errand.Outcome;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.run.Running;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import java.util.Optional;

/**
 * Whether the errand a pet is on goes on. The mirror of what lets an errand start, and as little
 * asked as it: the pet's capability must still offer the errand and its directive still permit it,
 * the errand itself must still want to go on, and where it goes must still be within the leash. A
 * pet is not taken off an errand because another of the same priority could start; that is for a higher one.
 */
public final class Continuation {
    private Continuation() {
    }

    /**
     * @param running the errand the pet is on
     * @param type that errand
     * @param offered whether the pet's capability still offers it
     * @param view the pet
     * @param permits what the pet is allowed to do
     * @return how the errand ends now, or nothing if it goes on: interrupted when the pet was taken off
     *         it by a change of capability or directive, and gave up when the errand no longer wants to
     *         go on, or what it was heading for is out of the pet's area
     */
    public static Optional<Outcome> endOf(Running running, ErrandType<?, ?> type, boolean offered, PetView view,
            Permits permits) {
        if (!offered) {
            return Optional.of(Outcome.interrupted("capability"));
        }
        if (!permits.allows(type.category())) {
            return Optional.of(Outcome.interrupted("directive"));
        }
        Check wanted = stillWanted(type, running, view);
        if (!wanted.ok()) {
            return Optional.of(Outcome.gaveUp(wanted.reason()));
        }
        return running.plan().destination(view)
            .filter(destination -> !view.anchor().admits(destination, false))
            .map(destination -> Outcome.gaveUp("out_of_leash"));
    }

    @SuppressWarnings("unchecked")
    private static <P extends Plan, T> Check stillWanted(ErrandType<P, T> type, Running running, PetView view) {
        return type.stillWanted((P) running.plan(), view, (T) running.settings().tuning());
    }
}
