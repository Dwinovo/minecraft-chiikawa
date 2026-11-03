package com.dwinovo.chiikawa.anim.pose;

import com.dwinovo.chiikawa.anim.controller.ControllerSnapshot;

/**
 * What the engine reads of an entity, captured once per frame, to pose it: the controllers'
 * snapshots, the clock they are sampled at, and the few numbers the input providers and
 * interceptors feed on. The caller fills it when the frame's state is captured (it lives in
 * the caller's render state) and hands it to {@link PoseComposer#compose}; the engine never
 * reads the live entity.
 *
 * <p>Putting an immutable snapshot of the controllers here (rather than the live animator)
 * keeps drawing a pure function of the captured state plus baked data: a second capture for
 * a preview of the same entity snapshots fresh values without disturbing the world's.
 */
public final class PoseFrame {
    /**
     * Per-controller snapshot in registration order. Composing the pose iterates this and
     * samples each controller into the shared pose buffer using its declared
     * {@link com.dwinovo.chiikawa.anim.controller.BlendMode}.
     */
    public ControllerSnapshot[] controllerSnapshots = EMPTY;
    /**
     * Pause-aware animation time captured with the frame. Composing must reuse this value so
     * controller state and pose sampling agree exactly for the snapshot.
     */
    public long animationTimeNs;
    /** Normalised walk speed — feeds Molang {@code query.ground_speed}. */
    public float walkSpeed;
    /**
     * Head yaw relative to body, in degrees, captured with the frame.
     *
     * <p>Stored here rather than recomputed from the entity's rotations at draw time because
     * a preview screen may overwrite those after capture, giving a difference that has
     * nothing to do with the entity's real head turn.
     */
    public float netHeadYaw;
    /** Head pitch in degrees, captured with the frame for the same reason. */
    public float headPitch;

    private static final ControllerSnapshot[] EMPTY = new ControllerSnapshot[0];

    /**
     * Whether the named controller is currently sampling the named animation (case-sensitive).
     * "Bone visible when controller X plays animation Y" — the most common visibility rule
     * (held props, prop-specific bones).
     *
     * @param controllerName e.g. {@code "action"}
     * @param animationName  short name (e.g. {@code "play_guitar"})
     */
    public boolean isPlaying(String controllerName, String animationName) {
        for (ControllerSnapshot snapshot : controllerSnapshots) {
            if (snapshot == null || !controllerName.equals(snapshot.name())) continue;
            if (snapshot.current() == null) return false;
            return animationName.equals(snapshot.current().animation().name);
        }
        return false;
    }

    /**
     * Whether <em>any</em> controller is currently sampling the named animation. Useful when
     * a rule shouldn't care which controller plays it (e.g. "show the guitar bone whenever
     * some controller — main, action, whatever — plays the {@code guitar} animation").
     *
     * <p>Checks both the controller's {@code current} and {@code previous}
     * channels. The {@code previous} check is what keeps a prop visible
     * during a fade-OUT crossfade — without it, the prop pops out the moment
     * the resolver picks a new animation, even though the body is still
     * mid-fade in the prop's pose. Equivalent to "is the named animation
     * still contributing to the pose buffer for any controller".
     */
    public boolean isAnyPlaying(String animationName) {
        for (ControllerSnapshot snapshot : controllerSnapshots) {
            if (snapshot == null) continue;
            if (snapshot.current() != null
                    && animationName.equals(snapshot.current().animation().name)) {
                return true;
            }
            if (snapshot.previous() != null
                    && animationName.equals(snapshot.previous().animation().name)) {
                return true;
            }
        }
        return false;
    }
}
