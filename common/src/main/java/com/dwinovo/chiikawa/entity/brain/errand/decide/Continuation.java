package com.dwinovo.chiikawa.entity.brain.errand.decide;

import com.dwinovo.chiikawa.entity.brain.errand.Check;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandType;
import com.dwinovo.chiikawa.entity.brain.errand.Outcome;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.run.Running;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import java.util.Optional;
import net.minecraft.core.GlobalPos;

/**
 * Whether the errand a pet is on goes on. The mirror of what lets an errand start, and as little
 * asked as it: the pet's capability must still offer the errand and its directive still permit it,
 * the errand itself must still want to go on, and it must still be within the leash: where it goes, for an
 * errand that goes somewhere, and the pet itself, for one that stays where it is. A pet is not taken off an
 * errand because another of the same priority could start; that is for a higher one.
 */
public final class Continuation {
    private Continuation() {
    }

    /**
     * @param running the errand the pet is on, which has the type it started as, whatever has been reloaded since
     * @param offered whether the pet's capability still offers it
     * @param view the pet
     * @param permits what the pet is allowed to do
     * @return how the errand ends now, or nothing if it goes on: interrupted when the pet was taken off
     *         it by a change of capability or directive, and gave up when the errand no longer wants to
     *         go on, or what it was heading for, or the pet itself when the errand heads nowhere, is out of the pet's area
     */
    public static Optional<Outcome> endOf(Running running, boolean offered, PetView view, Permits permits) {
        if (!offered) {
            return Optional.of(Outcome.interrupted("capability"));
        }
        if (!permits.allows(running.type().category())) {
            return Optional.of(Outcome.interrupted("directive"));
        }
        Check wanted = stillWanted(running.type(), running, view);
        if (!wanted.ok()) {
            return Optional.of(Outcome.gaveUp(wanted.reason()));
        }
        Optional<GlobalPos> destination = running.plan().destination(view);
        if (destination.isPresent()) {
            return view.anchor().admits(destination.get(), false) ? Optional.empty() : Optional.of(Outcome.gaveUp("out_of_leash"));
        }
        // An errand that goes nowhere, playing music where it stands, is not held to a place but to the pet: one carried
        // past the leash, as when its area moves away, leaves it, and wandering walks the pet back.
        return view.anchor().admits(view.pos(), false) ? Optional.empty() : Optional.of(Outcome.gaveUp("left_area"));
    }

    @SuppressWarnings("unchecked")
    private static <P extends Plan, T> Check stillWanted(ErrandType<P, T> type, Running running, PetView view) {
        return type.stillWanted((P) running.plan(), view, (T) running.settings().tuning());
    }
}
