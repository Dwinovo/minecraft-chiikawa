package com.dwinovo.chiikawa.entity.brain.errand.pastime;

import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestSources;

/**
 * What the tuning of every pastime has: how long a pet leaves the pastime alone once it has done it, which its file's
 * {@code done} rest points at, as {@code {"from": "chiikawa:pastime_cooldown"}}, so that the length lives in one place and
 * the world's setting can scale it.
 */
public interface PastimeTuning {
    /**
     * What {@link RestSources#PASTIME_COOLDOWN} comes to: the cooldown of the pastime the rest is about, which is the errand
     * its context names, divided by how much more often than the data says the world has pets pass the time. A pastime
     * that is not loaded, or that has no cooldown, rests for nothing.
     */
    RestSources.Resolver COOLDOWN = context -> context.key().flatMap(ErrandSettings::of).map(ErrandSettings::tuning)
        .filter(PastimeTuning.class::isInstance).map(PastimeTuning.class::cast)
        .map(tuning -> context.pastimes() > 0.0F ? Math.round(tuning.cooldownTicks() / context.pastimes()) : (long) tuning.cooldownTicks())
        .orElse(0L);

    /** @return the least time between two of this pastime, in a world where the pastimes are as often as the data says */
    int cooldownTicks();
}
