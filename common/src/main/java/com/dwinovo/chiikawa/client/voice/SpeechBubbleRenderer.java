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
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

/**
 * What a pet is saying, in a bubble over its head, for everyone near enough to have heard
 * it — whoever the pet belongs to. F1 hides it with the rest of the HUD, and it fades at
 * the end.
 *
 * <p>It always sits at the same height: one line above the spot where the game draws a
 * name tag, whether a name tag is showing there or not. So it clears a named pet's name
 * and never moves while it is up, however the player looks at the pet or walks about.
 *
 * <p>What it draws is taken from the pet with the rest of its render state, as the game
 * takes a name tag's text and spot, and drawn from that alone.
 */
public final class SpeechBubbleRenderer {
    /** How far above the name-tag spot the bubble sits, in blocks: one line of a name tag. */
    private static final float ABOVE_NAME = 0.28F;
    /** Text pixels to blocks, as the game draws a name tag. */
    private static final float SCALE = 0.025F;
    /** How long a line takes to fade at the end of its time, in ticks. */
    private static final float FADE_TICKS = 10.0F;

    /**
     * @return what the pet is saying this frame, or {@code null} when there is no bubble to draw
     */
    @Nullable
    public Saying extract(AbstractPet pet, float partialTick) {
        if (Minecraft.getInstance().options.hideGui) {
            return null;
        }
        return pet.getSpeech().map(speech -> {
            Vec3 nameTag = pet.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, pet.getViewYRot(partialTick));
            if (nameTag == null) {
                return null;
            }
            float left = speech.left(pet.tickCount, partialTick);
            return new Saying(Component.translatable(speech.line()).getString(), nameTag,
                Mth.clamp(left / FADE_TICKS, 0.0F, 1.0F));
        }).orElse(null);
    }

    public void render(Saying saying, PoseStack poseStack, MultiBufferSource bufferSource, Font font,
                       Quaternionf cameraOrientation) {
        Vec3 nameTag = saying.nameTag();
        poseStack.pushPose();
        poseStack.translate(nameTag.x, nameTag.y + 0.5 + ABOVE_NAME, nameTag.z);
        poseStack.mulPose(cameraOrientation);
        poseStack.scale(SCALE, -SCALE, SCALE);
        WorldSurface surface = new WorldSurface(poseStack, bufferSource, font, saying.alpha());
        Bubble bubble = new Bubble(saying.words());
        bubble.draw(surface, -bubble.width(surface) / 2, -bubble.height(surface) - Bubble.TAIL, 0);
        poseStack.popPose();
    }

    /**
     * A line over a pet's head, as it is drawn this frame.
     *
     * @param words   what it says, in the player's language
     * @param nameTag where the game draws the pet's name tag, relative to the pet
     * @param alpha   how much of the bubble shows, from 0 as it fades out to 1
     */
    public record Saying(String words, Vec3 nameTag, float alpha) {
    }
}
