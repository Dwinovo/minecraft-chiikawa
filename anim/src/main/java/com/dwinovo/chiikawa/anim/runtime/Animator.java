package com.dwinovo.chiikawa.anim.runtime;

import com.dwinovo.chiikawa.anim.baked.BakedAnimation;
import com.dwinovo.chiikawa.anim.controller.ControllerConfig;
import com.dwinovo.chiikawa.anim.controller.ControllerInstance;
import com.dwinovo.chiikawa.anim.controller.ControllerRoster;
import com.dwinovo.chiikawa.anim.controller.ControllerSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-entity animation state. Holds one {@link ControllerInstance} per
 * {@link ControllerConfig} of the {@link ControllerRoster} it is advanced with.
 *
 * <h2>Controller iteration</h2>
 * Controllers run in registration order (the roster's order). Each controller
 * writes into the shared pose buffer when the pose is composed, so a controller
 * registered later sees and may override / blend onto values written by
 * earlier ones — this is the pillar of the GeckoLib-style pipeline model: the
 * registration order <em>is</em> the priority.
 *
 * <h2>Why initialisation is lazy</h2>
 * The roster belongs to whatever draws the entity, which can be a different
 * place (and a different side of the game) from where the entity makes its
 * animator, so the animator is built empty and the first {@link #advance}
 * materialises the controller instances. An animator never advanced stays
 * dormant for its entire lifetime — it costs only a single empty {@code List}.
 *
 * <h2>One-shot triggers</h2>
 * {@link #playOnce} dispatches an externally-driven one-shot animation to the
 * named controller (typically {@code "action"} or {@code "reaction"}). The
 * triggered animation overrides the controller's state handler until its
 * baked duration elapses (once, even if the animation file loops), then the
 * handler resumes. Mirrors GeckoLib's
 * {@code triggerAnimation}.
 *
 * @param <I> what the roster's handlers pick by, see
 *            {@link com.dwinovo.chiikawa.anim.controller.ControllerHandler}
 */
public final class Animator<I> {

    /**
     * Stable per-entity offset latched by {@link #latchPhaseSeed} and propagated
     * to controller instances. {@link Long#MIN_VALUE} = unset.
     */
    private long phaseSeed = Long.MIN_VALUE;
    private List<ControllerInstance<I>> controllers = List.of();

    /**
     * Brings every controller up to date for one frame and captures what each plays: the
     * single call a frame makes, in the order that keeps it right (materialise on the first
     * call, latch the phase, drop what a reload made stale, run the handlers, snapshot).
     *
     * @param roster     the controllers to run; only the first call materialises them,
     *                   re-passing the same roster afterwards is a no-op
     * @param input      what the handlers pick by this frame
     * @param uniqueness a number stable for this entity and different between entities, so
     *                   two created at the same instant do not blink in lockstep
     * @param nowNs      animation-clock time of the frame
     * @param bakeStamp  stamp of the bake the entity's model has now, so animations from an
     *                   earlier bake (a resource reload) are dropped; {@code 0} when
     *                   there is none to compare with
     * @return one immutable snapshot per controller, in registration order
     */
    public ControllerSnapshot[] advance(ControllerRoster<I> roster, I input, long uniqueness, long nowNs,
                                        long bakeStamp) {
        ensureInitialised(roster);
        latchPhaseSeed(uniqueness, nowNs);
        if (bakeStamp != 0L) {
            for (ControllerInstance<I> c : controllers) c.clearStale(bakeStamp);
        }
        for (ControllerInstance<I> c : controllers) c.tick(input, nowNs);
        ControllerSnapshot[] out = new ControllerSnapshot[controllers.size()];
        for (int i = 0; i < out.length; i++) out[i] = controllers.get(i).snapshot();
        return out;
    }

    /**
     * Latches the entity-stable phase seed used to anchor non-fading
     * looping animations across two entities (so two entities created at the same
     * instant don't blink in lockstep). Subsequent calls are no-ops to keep
     * the seed stable for the lifetime of the animator.
     *
     * <p>The seed value is {@code nowNs - uniqueness * 1ms} so
     * that the elapsed time at frame {@code t} is uniformly distributed
     * across [0, ~10s] for a 16-bit uniqueness input.
     */
    private void latchPhaseSeed(long uniqueness, long nowNs) {
        if (phaseSeed != Long.MIN_VALUE) return;
        long offsetMs = Math.floorMod(uniqueness, 10_000L);
        phaseSeed = nowNs - offsetMs * 1_000_000L;
        for (ControllerInstance<I> c : controllers) c.setPhaseSeed(phaseSeed);
    }

    /**
     * Materialises one {@link ControllerInstance} per {@link ControllerConfig} of
     * the roster, but only on the first call. Re-invoking with the same roster
     * (it is shared and static once built) is a no-op.
     */
    private void ensureInitialised(ControllerRoster<I> roster) {
        if (!controllers.isEmpty()) return;
        List<ControllerConfig<I>> configs = roster.configs();
        if (configs.isEmpty()) return;
        List<ControllerInstance<I>> instances = new ArrayList<>(configs.size());
        for (ControllerConfig<I> config : configs) {
            ControllerInstance<I> instance = new ControllerInstance<>(config);
            if (phaseSeed != Long.MIN_VALUE) instance.setPhaseSeed(phaseSeed);
            instances.add(instance);
        }
        controllers = List.copyOf(instances);
    }

    /**
     * Trigger a one-shot animation on the named controller. The trigger
     * overrides the controller's state handler until {@code animation}'s
     * baked duration elapses, playing once even if the animation file loops
     * (or, for {@code HOLD_ON_LAST_FRAME}, until the
     * trigger is replaced or cleared).
     *
     * <p>For gameplay events that arrive from outside the frame loop, such as a
     * synced action or reaction: the receiver looks up the matching
     * {@link BakedAnimation} and calls this on the controller meant for it.
     */
    public void playOnce(String controllerName, BakedAnimation animation, long nowNs) {
        ControllerInstance<I> c = byName(controllerName);
        if (c != null) c.playOnce(animation, nowNs);
    }

    /** Look up a controller by name. {@code null} for unknown names. */
    private ControllerInstance<I> byName(String name) {
        for (ControllerInstance<I> c : controllers) {
            if (c.name().equals(name)) return c;
        }
        return null;
    }
}
