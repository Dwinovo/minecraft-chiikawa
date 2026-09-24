package com.dwinovo.chiikawa.anim.render;

import org.joml.Quaternionf;
import org.joml.Vector3f;

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

    /**
     * The three angles, in radians, that {@link #of} turns by to give {@code turn}: the way
     * back, for a turn worked out as a whole that has to go into a bone's angles. (JOML's own
     * {@code getEulerAnglesZYX} does not come back to the turn it was given in the version the
     * game ships, so this is worked out here, from the same order.)
     *
     * @return {@code into}, holding x, y and z
     */
    public static Vector3f angles(Quaternionf turn, Vector3f into) {
        float x = turn.x, y = turn.y, z = turn.z, w = turn.w;
        // The rows of Rz·Ry·Rx that give the angles back.
        float r00 = 1 - 2 * (y * y + z * z);
        float r10 = 2 * (x * y + w * z);
        float r20 = 2 * (x * z - w * y);
        float r21 = 2 * (y * z + w * x);
        float r22 = 1 - 2 * (x * x + y * y);
        float sinY = Math.max(-1f, Math.min(1f, -r20));
        if (Math.abs(sinY) > 0.99999f) {
            // Straight up or down: X and Z turn about the same axis, so Z takes none of it.
            float r11 = 1 - 2 * (x * x + z * z);
            float r12 = 2 * (y * z - w * x);
            return into.set((float) Math.atan2(-r12, r11), (float) Math.asin(sinY), 0f);
        }
        return into.set((float) Math.atan2(r21, r22), (float) Math.asin(sinY), (float) Math.atan2(r10, r00));
    }
}
