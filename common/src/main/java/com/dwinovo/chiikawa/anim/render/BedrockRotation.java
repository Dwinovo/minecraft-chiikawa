package com.dwinovo.chiikawa.anim.render;

import org.joml.Quaternionf;

/**
 * How a Bedrock model turns a bone or a cube by its three angles: about X first, then Y,
 * then Z, each in the frame the previous turns left — the ZYX order Blockbench draws with,
 * and vanilla's {@code ModelPart} and GeckoLib turn by. A head that looks to one side and
 * up turns aside and then tips back in its own frame, so it stays level; taken the other
 * way round it tips about the body's axis instead and lolls over to one side.
 *
 * <p>Everything that walks the bones uses this one order, so a model is drawn as it was
 * made, and what hangs from a bone (a held item, a bag) follows it exactly.
 */
public final class BedrockRotation {
    private BedrockRotation() {
    }

    /**
     * Sets {@code into} to the turn by these angles, in radians.
     *
     * @return {@code into}
     */
    public static Quaternionf of(Quaternionf into, float x, float y, float z) {
        return into.rotationZYX(z, y, x);
    }
}
