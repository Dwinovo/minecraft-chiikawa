package com.dwinovo.chiikawa.entity.brain.errand.run;

/**
 * Something a pet does on the side while it is on an errand whose settings name it, such as chatting while it
 * wanders. Not a step: it is not under the errand's routine and does not end it, begins nowhere and has nothing to
 * let go of, and it never moves the pet, see {@link Aside}. The executor ticks it once for every tick the errand
 * is under way.
 */
@FunctionalInterface
public interface Overlay {
    /** One tick beside the errand. */
    void tick(Aside aside);
}
