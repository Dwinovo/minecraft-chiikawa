package com.dwinovo.chiikawa.client.voice;

import com.dwinovo.chiikawa.client.ui.mc.WorldSurface;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.ui.widget.Bubble;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

/**
 * What a pet is saying, in a bubble over its head, for everyone near enough to have heard
 * it — whoever the pet belongs to. F1 hides it with the rest of the HUD, and it fades at
 * the end.
 *
 * <p>It always sits at the same height: one line above the spot where the game draws a
 * name tag, whether a name tag is showing there or not. So it clears a named pet's name
 * and never moves while it is up, however the player looks at the pet or walks about.
 */
public final class SpeechBubbleRenderer {
    /** How far above the name-tag spot the bubble sits, in blocks: one line of a name tag. */
    private static final float ABOVE_NAME = 0.28F;
    /** Text pixels to blocks, as the game draws a name tag. */
    private static final float SCALE = 0.025F;
    /** How long a line takes to fade at the end of its time, in ticks. */
    private static final float FADE_TICKS = 10.0F;

    public void render(AbstractPet pet, PoseStack poseStack, MultiBufferSource bufferSource, Font font,
                       Quaternionf cameraOrientation, float partialTick) {
        if (Minecraft.getInstance().options.hideGui) {
            return;
        }
        pet.getSpeech().ifPresent(speech -> {
            // 1.20.4 puts a name tag at a height of its own rather than at an attachment point.
            poseStack.pushPose();
            poseStack.translate(0.0F, pet.getNameTagOffsetY() + ABOVE_NAME, 0.0F);
            poseStack.mulPose(cameraOrientation);
            // Before 1.21 the camera's rotation is a half turn about y short of the later one,
            // so every axis is flipped to land in the same frame; flipping x alone, as this
            // version's own name tag does, would leave z pointing away and bury the text in its card.
            poseStack.scale(-SCALE, -SCALE, -SCALE);
            float left = speech.left(pet.tickCount, partialTick);
            WorldSurface surface = new WorldSurface(poseStack, bufferSource, font, Mth.clamp(left / FADE_TICKS, 0.0F, 1.0F));
            Bubble bubble = new Bubble(Component.translatable(speech.line()).getString());
            bubble.draw(surface, -bubble.width(surface) / 2, -bubble.height(surface) - Bubble.TAIL, 0);
            poseStack.popPose();
        });
    }
}
