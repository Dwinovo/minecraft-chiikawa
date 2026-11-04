package com.dwinovo.chiikawa.client.voice;

import com.dwinovo.chiikawa.client.settings.ClientSettingsStore;
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
 * it — whoever the pet belongs to. F1 hides it with the rest of the HUD, a player who has
 * turned speech bubbles off in the settings does not get it at all, and it fades at the end.
 * The pet opens its mouth either way: that is the pet's, not the bubble's.
 *
 * <p>It is drawn on a surface turned to the camera and scaled to text pixels, as the game draws a name tag, and it sits on
 * the pet's head: just above the top of it, which is the top of the pet's hitbox. While the game draws a name tag, which it
 * does from half a block above that point and one line of text down, it sits just above the name instead, so it moves up by a name tag as the name appears and down
 * as it goes. Whether a name is drawn is the renderer's to say, see {@code ChiikawaEntityRenderer#shouldShowName}, and is
 * handed in with the pet; this never asks the pet. Apart from that it never moves while it is up, however the player looks
 * at the pet or walks about.
 */
public final class SpeechBubbleRenderer {
    /** How long a line takes to fade at the end of its time, in ticks. */
    private static final float FADE_TICKS = 10.0F;
    /** Text pixels to blocks, as the game draws a name tag. */
    private static final float SCALE = 0.025F;
    /** How far above the top of the pet's head it sits when no name tag is drawn, in blocks. */
    private static final float ABOVE_HEAD = 0.1F;
    /** How far above the top of the pet the game starts drawing a name tag from, in blocks: the top of the name. */
    private static final float NAME_TAG_RISE = 0.5F;
    /** How far above the name it sits when one is drawn, in blocks: four text pixels. */
    private static final float ABOVE_NAME = 4 * SCALE;

    /** @param nameShown whether the game draws the pet's name tag, which the bubble hangs over */
    public void render(AbstractPet pet, boolean nameShown, PoseStack poseStack, MultiBufferSource bufferSource, Font font,
                       Quaternionf cameraOrientation, float partialTick) {
        if (Minecraft.getInstance().options.hideGui || !ClientSettingsStore.current().speechBubbles()) {
            return;
        }
        pet.getSpeech().ifPresent(speech -> {
            float left = speech.left(pet.tickCount, partialTick);
            Bubble bubble = new Bubble(Component.translatable(speech.line(),
                speech.subject().map(Component::translatable).orElse(Component.empty())).getString());
            // 1.20.1 puts a name tag at a height of its own rather than at an attachment point: the top of the hitbox, and the rise.
            float top = pet.getNameTagOffsetY() - NAME_TAG_RISE;
            poseStack.pushPose();
            poseStack.translate(0.0F, top + (nameShown ? NAME_TAG_RISE + ABOVE_NAME : ABOVE_HEAD), 0.0F);
            poseStack.mulPose(cameraOrientation);
            // Text pixels from here on, with y running down as on a screen and z towards the
            // camera. Before 1.21 the camera's rotation is a half turn about y short of the later
            // one, so every axis is flipped to land in the same frame; flipping x alone, as this
            // version's own name tag does, would leave z pointing away and bury the text in its card.
            poseStack.scale(-SCALE, -SCALE, -SCALE);
            WorldSurface surface = new WorldSurface(poseStack, bufferSource, font, Mth.clamp(left / FADE_TICKS, 0.0F, 1.0F));
            bubble.draw(surface, -bubble.width(surface) / 2, -bubble.height(surface) - Bubble.TAIL, 0);
            poseStack.popPose();
        });
    }
}
