package com.dwinovo.chiikawa.entity.job.api;

import com.dwinovo.chiikawa.entity.AbstractPet;
import net.minecraft.world.entity.ai.Brain;

/**
 * A pet job descriptor. Brain wiring is now <em>static</em> — every job's
 * activities are registered once at brain construction (see
 * {@link AbstractPet#makeBrain}); jobs don't own brain initialisation
 * lifecycle anymore. The job interface only exposes:
 *
 * <ul>
 *   <li>identity ({@link #getId} / {@link #getPriority})</li>
 *   <li>auto-assignment ({@link #canAssume}) — used by
 *       {@code refreshJobFromMainhand} when an item-tag-driven recompute
 *       is needed; manual job switching can bypass this entirely by
 *       writing {@code AbstractPet.setPetJobId} directly.</li>
 *   <li>per-tick activity selection ({@link #tickBrain}) — runs only when
 *       this job is the active one, choosing which of its registered
 *       activities should be active this tick.</li>
 * </ul>
 *
 * <p>No more {@code initBrain} on jobs — adding a job means: register its
 * activities once in {@code AbstractPet.makeBrain}, write a tickBrain that
 * picks among them. No brain rebuild on job change.
 */
public interface IPetJob {
    int getId();

    int getPriority();

    boolean canAssume(AbstractPet pet);

    void tickBrain(AbstractPet pet, Brain<AbstractPet> brain);
}
