package com.dwinovo.chiikawa.entity.brain.errand;

import java.util.List;

/**
 * What an errand answers when it is asked what it offers: the ways the pet could set out on it, or the
 * check that failed. One answer and not two questions, so what keeps an errand from starting and what
 * the debug command says is keeping it are the same condition and cannot drift apart. Never an empty
 * list passed off as an answer: nothing to offer is a reason.
 *
 * @param <P> the shape of the errand's plans
 */
public sealed interface Offers<P extends Plan> {
    /** @return the ways the pet could set out, none when {@link #check} failed */
    List<Offer<P>> list();

    /** @return {@link Check#OK} when there are offers, and otherwise what keeps the errand from starting */
    Check check();

    /** @return whether the errand has nothing to offer */
    default boolean isEmpty() {
        return list().isEmpty();
    }

    /**
     * @param offers the ways to go about it, at least one
     * @return an answer of these offers
     */
    static <P extends Plan> Offers<P> of(List<Offer<P>> offers) {
        return new Some<>(offers);
    }

    /** @return an answer of the only way to go about it */
    static <P extends Plan> Offers<P> of(Offer<P> offer) {
        return new Some<>(List.of(offer));
    }

    /**
     * @param why the check that failed, with its reason
     * @return an answer of no offers
     */
    static <P extends Plan> Offers<P> none(Check why) {
        return new None<>(why);
    }

    /**
     * @param list the offers
     * @param <P> the shape of the plans
     */
    record Some<P extends Plan>(List<Offer<P>> list) implements Offers<P> {
        public Some {
            if (list.isEmpty()) {
                throw new IllegalArgumentException("No offers is not an answer, say why with Offers.none");
            }
            list = List.copyOf(list);
        }

        @Override
        public Check check() {
            return Check.OK;
        }
    }

    /**
     * @param check why there is nothing to offer
     * @param <P> the shape of the plans
     */
    record None<P extends Plan>(Check check) implements Offers<P> {
        public None {
            if (check.ok()) {
                throw new IllegalArgumentException("Nothing to offer needs a reason that failed");
            }
        }

        @Override
        public List<Offer<P>> list() {
            return List.of();
        }
    }
}
