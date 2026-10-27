package com.dwinovo.chiikawa.client.voice;

import com.dwinovo.chiikawa.client.ui.mc.WorldSurface;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.ui.DrawSurface;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The spot over a pet's head that what it says and what is on its mind are drawn at, and the way to draw there: a surface
 * turned to the camera and scaled to text pixels, as the game draws a name tag.
 *
 * <p>It always sits at the same height: one line above the spot where the game draws a name tag, whether a name tag is
 * showing there or not. So it clears a named pet's name and never moves while it is up, however the player looks at the
 * pet or walks about.
 *
 * <p>The spot is taken from the pet with the rest of its render state, as the game takes a name tag's, and drawn from that
 * alone.
 */
final class OverHead {
    /** How far above the name-tag spot it sits, in blocks: one line of a name tag. */
    private static final float ABOVE_NAME = 0.28F;
    /** Text pixels to blocks, as the game draws a name tag. */
    private static final float SCALE = 0.025F;

    private OverHead() {
    }

    /**
     * @return where the game draws the pet's name tag, relative to the pet, or {@code null} when the pet has none
     */
    @Nullable
    static Vec3 spot(AbstractPet pet, float partialTick) {
        return pet.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, pet.getYRot(partialTick));
    }

    /**
     * @param nameTag the pet's {@link #spot}
     * @param alpha how much of what is drawn shows, from 0 for none of it to 1 for all
     * @param drawing draws on the surface, whose origin is the spot and whose y runs down; what is drawn hangs above it
     */
    static void draw(Vec3 nameTag, PoseStack poseStack, SubmitNodeCollector collector, Font font,
                     CameraRenderState camera, float alpha, Consumer<DrawSurface> drawing) {
        poseStack.pushPose();
        poseStack.translate(nameTag.x, nameTag.y + 0.5 + ABOVE_NAME, nameTag.z);
        poseStack.mulPose(camera.orientation);
        poseStack.scale(SCALE, -SCALE, SCALE);
        drawing.accept(new WorldSurface(poseStack, collector, font, alpha));
        poseStack.popPose();
    }
}
