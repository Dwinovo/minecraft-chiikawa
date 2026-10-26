package com.dwinovo.chiikawa.client.voice;

import com.dwinovo.chiikawa.client.ui.mc.WorldSurface;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.ui.DrawSurface;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * The spot over a pet's head that what it says and what is on its mind are drawn at, and the way to draw there: a surface
 * turned to the camera and scaled to text pixels, as the game draws a name tag.
 *
 * <p>It always sits at the same height: one line above the spot where the game draws a name tag, whether a name tag is
 * showing there or not. So it clears a named pet's name and never moves while it is up, however the player looks at the
 * pet or walks about.
 */
final class OverHead {
    /** How far above the name-tag spot it sits, in blocks: one line of a name tag. */
    private static final float ABOVE_NAME = 0.28F;
    /** Text pixels to blocks, as the game draws a name tag. */
    private static final float SCALE = 0.025F;

    private OverHead() {
    }

    /**
     * @param alpha how much of what is drawn shows, from 0 for none of it to 1 for all
     * @param drawing draws on the surface, whose origin is the spot and whose y runs down; what is drawn hangs above it
     */
    static void draw(AbstractPet pet, PoseStack poseStack, MultiBufferSource bufferSource, Font font,
                     Quaternionf cameraOrientation, float partialTick, float alpha, Consumer<DrawSurface> drawing) {
        Vec3 nameTag = pet.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, pet.getViewYRot(partialTick));
        if (nameTag == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(nameTag.x, nameTag.y + 0.5 + ABOVE_NAME, nameTag.z);
        poseStack.mulPose(cameraOrientation);
        poseStack.scale(SCALE, -SCALE, SCALE);
        drawing.accept(new WorldSurface(poseStack, bufferSource, font, alpha));
        poseStack.popPose();
    }
}
