package com.dwinovo.chiikawa.entity.brain.errand.run;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The steps of one errand, in order. Built once each time the errand starts and used up by it: it
 * remembers where it has got to.
 *
 * <p>A routine is made of parts that give their steps one at a time, so a step is made when its turn
 * comes and sees the world as it is then, not as it was when the pet set out: {@link #step} for one,
 * {@link #repeatWhile} for a few that go round while something holds.
 */
public final class Routine {
    /** A part of a routine: gives the steps it is made of one at a time, then nothing. */
    @FunctionalInterface
    public interface Part {
        /**
         * @param handle the pet, for a part that goes on while something about it holds
         * @return the next step, or nothing when the part is over
         */
        Optional<Step> next(Handle handle);
    }

    private final List<Part> parts;
    private int at;

    private Routine(List<Part> parts) {
        this.parts = List.copyOf(parts);
    }

    /** @return a routine of these parts, one after the other */
    public static Routine of(Part... parts) {
        return new Routine(List.of(parts));
    }

    /** @return a routine of one step that is already made */
    public static Routine single(Step step) {
        return of(step(() -> step));
    }

    /** @return a part of one step, made when it is its turn */
    public static Part step(Supplier<? extends Step> step) {
        return new Once(step);
    }

    /**
     * A part that goes round its steps for as long as the condition holds, asking before the first
     * round as well as between rounds.
     *
     * @param more whether to go round again
     * @param body the steps of a round, in order, each made when it is its turn
     */
    @SafeVarargs
    public static Part repeatWhile(Predicate<Handle> more, Supplier<? extends Step>... body) {
        return new Rounds(more, List.of(body));
    }

    /**
     * @param handle the pet
     * @return the next step, or nothing when the routine is over
     */
    public Optional<Step> next(Handle handle) {
        while (at < parts.size()) {
            Optional<Step> step = parts.get(at).next(handle);
            if (step.isPresent()) {
                return step;
            }
            at++;
        }
        return Optional.empty();
    }

    private static final class Once implements Part {
        private final Supplier<? extends Step> step;
        private boolean given;

        private Once(Supplier<? extends Step> step) {
            this.step = step;
        }

        @Override
        public Optional<Step> next(Handle handle) {
            if (given) {
                return Optional.empty();
            }
            given = true;
            return Optional.of(step.get());
        }
    }

    private static final class Rounds implements Part {
        private final Predicate<Handle> more;
        private final List<Supplier<? extends Step>> round;
        /** The step of the round to give next; the round's length when it is time to ask. */
        private int next;
        private boolean over;

        private Rounds(Predicate<Handle> more, List<Supplier<? extends Step>> round) {
            if (round.isEmpty()) {
                throw new IllegalArgumentException("A round of nothing never ends");
            }
            this.more = more;
            this.round = round;
            this.next = round.size();
        }

        @Override
        public Optional<Step> next(Handle handle) {
            if (over) {
                return Optional.empty();
            }
            if (next == round.size()) {
                if (!more.test(handle)) {
                    over = true;
                    return Optional.empty();
                }
                next = 0;
            }
            return Optional.of(round.get(next++).get());
        }
    }
}
