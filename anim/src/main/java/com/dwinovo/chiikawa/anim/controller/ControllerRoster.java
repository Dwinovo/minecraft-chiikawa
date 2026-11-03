package com.dwinovo.chiikawa.anim.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The controllers one kind of entity runs, in the order they run: the shared, immutable
 * description that every entity's {@link com.dwinovo.chiikawa.anim.runtime.Animator}
 * materialises into its own {@link ControllerInstance}s. Registration order is priority —
 * controllers added later override or blend on top of earlier ones in the shared pose
 * buffer.
 *
 * <p>Two controllers must not share a name: names address the one-shot triggers of
 * {@link com.dwinovo.chiikawa.anim.runtime.Animator#playOnce}.
 *
 * @param <I> what the controllers' handlers pick by, see {@link ControllerHandler}
 */
public final class ControllerRoster<I> {

    private final List<ControllerConfig<I>> configs = new ArrayList<>();

    /** Appends a controller; it runs after every one added before it. */
    public void add(ControllerConfig<I> config) {
        for (ControllerConfig<I> existing : configs) {
            if (existing.name().equals(config.name())) {
                throw new IllegalStateException("duplicate controller name: " + config.name());
            }
        }
        configs.add(config);
    }

    /** The controllers in registration order; a live read-only view. */
    public List<ControllerConfig<I>> configs() {
        return Collections.unmodifiableList(configs);
    }
}
