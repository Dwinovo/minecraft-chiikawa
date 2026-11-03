package com.dwinovo.chiikawa.anim.pose;

import com.dwinovo.chiikawa.anim.molang.MolangContext;

/**
 * Default {@link BoneInputProvider} that wires the per-frame state values the
 * baseline pet animation depends on:
 *
 * <ul>
 *   <li>{@code query.ground_speed} ← {@link PoseFrame#walkSpeed}</li>
 * </ul>
 *
 * <p>{@code query.anim_time} is intentionally not set here — that slot is
 * filled by {@link com.dwinovo.chiikawa.anim.runtime.PoseSampler} per channel
 * during sampling.
 */
public final class BasicMolangInputProvider implements BoneInputProvider {

    @Override
    public void fill(PoseFrame frame, MolangContext ctx) {
        ctx.vars[MolangContext.SLOT_GROUND_SPEED] = frame.walkSpeed;
    }
}
