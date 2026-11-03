package com.dwinovo.chiikawa.anim.runtime;

import com.dwinovo.chiikawa.anim.baked.BakedAnimation;
import com.dwinovo.chiikawa.anim.controller.ControllerConfig;
import com.dwinovo.chiikawa.anim.controller.ControllerInstance;
import com.dwinovo.chiikawa.anim.controller.ControllerRoster;
import com.dwinovo.chiikawa.anim.controller.ControllerSnapshot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
 * {@link #playOnce} asks for an externally-driven one-shot animation on the
 * named controller (typically {@code "action"} or {@code "reaction"}) <em>by
 * name</em>, the way an entity starts a vanilla {@code AnimationState} without
 * knowing the clip: the request is kept, and the next {@link #advance} resolves
 * the names against the {@link AnimationSource} it is given. A request made before
 * the first advance is therefore not lost, whatever side of the game made it. The
 * triggered animation overrides the controller's state handler until its
 * baked duration elapses (once, even if the animation file loops), then the
 * handler resumes. Mirrors GeckoLib's
 * {@code triggerAnimation}.
 *
 * @param <I> what the roster's handlers pick by, see
 *            {@link com.dwinovo.chiikawa.anim.controller.ControllerHandler}
 */
public final class Animator<I> {

    private static final Logger LOG = LoggerFactory.getLogger(Animator.class);

    /** A one-shot asked for by name and not yet resolved: the candidates, and when it was asked for. */
    private record Request(List<String> candidates, long requestedNs) {
    }

    /**
     * Stable per-entity offset latched by {@link #latchPhaseSeed} and propagated
     * to controller instances. {@link Long#MIN_VALUE} = unset.
     */
    private long phaseSeed = Long.MIN_VALUE;
    private List<ControllerInstance<I>> controllers = List.of();
    /** The latest request for each controller, resolved and cleared by the next {@link #advance}. */
    private final Map<String, Request> requests = new LinkedHashMap<>();

    /**
     * Brings every controller up to date for one frame and captures what each plays: the
     * single call a frame makes, in the order that keeps it right (materialise on the first
     * call, latch the phase, drop what a reload made stale, resolve the requests made since
     * the last frame, run the handlers, snapshot).
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
     * @param animations where the names of {@link #playOnce} requests are resolved
     * @return one immutable snapshot per controller, in registration order
     */
    public ControllerSnapshot[] advance(ControllerRoster<I> roster, I input, long uniqueness, long nowNs,
                                        long bakeStamp, AnimationSource animations) {
        ensureInitialised(roster);
        latchPhaseSeed(uniqueness, nowNs);
        if (bakeStamp != 0L) {
            for (ControllerInstance<I> c : controllers) c.clearStale(bakeStamp);
        }
        resolveRequests(animations);
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
     * Ask for a one-shot animation on the named controller: the first of the candidates
     * (short names) that {@link AnimationSource} has at the next {@link #advance}. The trigger
     * overrides the controller's state handler until the animation's baked duration elapses,
     * playing once even if the animation file loops (or, for {@code HOLD_ON_LAST_FRAME}, until
     * the trigger is replaced or cleared). It starts at {@code nowNs}, the time of the request,
     * not of the frame that resolves it.
     *
     * <p>For gameplay events that arrive from outside the frame loop, such as a synced action or
     * reaction. A later request for the same controller before the next advance replaces this
     * one; a controller the roster does not have ignores it; when none of the candidates exists
     * nothing plays and the miss is logged once.
     */
    public void playOnce(String controllerName, List<String> candidates, long nowNs) {
        requests.put(controllerName, new Request(List.copyOf(candidates), nowNs));
    }

    /** Hands every pending request to its controller, as the animation its candidates name. */
    private void resolveRequests(AnimationSource animations) {
        if (requests.isEmpty()) return;
        for (Map.Entry<String, Request> entry : requests.entrySet()) {
            ControllerInstance<I> controller = byName(entry.getKey());
            if (controller == null) continue;
            Request request = entry.getValue();
            BakedAnimation animation = animations.firstOf(request.candidates());
            if (animation == null) {
                LOG.debug("[chiikawa-anim] no animation on controller '{}' among {}", entry.getKey(),
                        request.candidates());
                continue;
            }
            controller.playOnce(animation, request.requestedNs());
        }
        requests.clear();
    }

    /** Look up a controller by name. {@code null} for unknown names. */
    private ControllerInstance<I> byName(String name) {
        for (ControllerInstance<I> c : controllers) {
            if (c.name().equals(name)) return c;
        }
        return null;
    }
}
