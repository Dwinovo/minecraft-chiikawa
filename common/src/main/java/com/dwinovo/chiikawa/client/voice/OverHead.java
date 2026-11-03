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
 * <p>It sits on the pet's head: just above the top of it, where the game puts the {@link EntityAttachment#NAME_TAG}
 * attachment point, which is the top of the pet's hitbox. While the game draws a name tag, which it does from half a block above
 * that point and one line of text down, it sits just above the name instead, so it moves up by a name tag as the name appears
 * and down as it goes. Whether a name is drawn is the render state's to say, {@code EntityRenderState#nameTag}, and is handed
 * in with what is to be drawn. Apart from that it never moves while it is up, however the player looks at the pet or walks
 * about.
 *
 * <p>The spot is taken from the pet with the rest of its render state, as the game takes a name tag's, and drawn from that
 * alone.
 */
final class OverHead {
    /** Text pixels to blocks, as the game draws a name tag. */
    private static final float SCALE = 0.025F;
    /** How far above the top of the pet's head it sits when no name tag is drawn, in blocks. */
    private static final float ABOVE_HEAD = 0.1F;
    /** How far above the name-tag attachment point the game starts drawing a name tag from, in blocks: the top of the name. */
    private static final float NAME_TAG_RISE = 0.5F;
    /** How far above the name it sits when one is drawn, in blocks: four text pixels. */
    private static final float ABOVE_NAME = 4 * SCALE;

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
     * @param nameShown whether the game draws the pet's name tag, which the spot clears
     * @param alpha how much of what is drawn shows, from 0 for none of it to 1 for all
     * @param drawing draws on the surface, whose origin is the spot and whose y runs down; what is drawn hangs above it
     */
    static void draw(Vec3 nameTag, boolean nameShown, PoseStack poseStack, SubmitNodeCollector collector, Font font,
                     CameraRenderState camera, float alpha, Consumer<DrawSurface> drawing) {
        poseStack.pushPose();
        poseStack.translate(nameTag.x, nameTag.y + (nameShown ? NAME_TAG_RISE + ABOVE_NAME : ABOVE_HEAD), nameTag.z);
        poseStack.mulPose(camera.orientation);
        poseStack.scale(SCALE, -SCALE, SCALE);
        drawing.accept(new WorldSurface(poseStack, collector, font, alpha));
        poseStack.popPose();
    }
}
