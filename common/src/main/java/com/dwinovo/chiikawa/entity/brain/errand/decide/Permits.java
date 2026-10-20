package com.dwinovo.chiikawa.entity.brain.errand.decide;

import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;

/**
 * What the pet is allowed to do at all, whatever it would fancy: today the owner's directive, which
 * permits or forbids whole categories of errand, and later the owner's own switches. Asked before an
 * errand is asked what it offers, so a personality or a score can never make a pet do what it was told not to.
 */
@FunctionalInterface
public interface Permits {
    /** @return whether errands of this category may be started, and gone on with */
    boolean allows(ErrandCategory category);
}
