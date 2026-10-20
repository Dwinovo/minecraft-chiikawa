package com.dwinovo.chiikawa.entity.brain.errand.view;

import java.util.function.Supplier;

/**
 * A fact of the pet that is worked out when it is first asked for and kept after: a view answers
 * the same to every errand that asks within one evaluation, and costs nothing for what none asks.
 */
final class Lazy<T> {
    private final Supplier<T> source;
    private T value;
    private boolean known;

    Lazy(Supplier<T> source) {
        this.source = source;
    }

    T get() {
        if (!known) {
            value = source.get();
            known = true;
        }
        return value;
    }
}
