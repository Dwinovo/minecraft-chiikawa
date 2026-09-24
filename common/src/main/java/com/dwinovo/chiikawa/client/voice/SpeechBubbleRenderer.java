package com.dwinovo.chiikawa.client.voice;

import com.dwinovo.chiikawa.anim.render.ChiikawaRenderState;
import com.dwinovo.chiikawa.anim.render.PetData;
import com.dwinovo.chiikawa.client.ui.mc.WorldSurface;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.ui.widget.Bubble;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.phys.Vec3;

/**
 * What a pet is saying, in a bubble over its head, for everyone near enough to have heard
 * it — whoever the pet belongs to. F1 hides it with the rest of the HUD, and it fades at
 * the end.
 *
 * <p>It always sits at the same height: one line above the spot where the game draws a
 * name tag, whether a name tag is showing there or not. So it clears a named pet's name
 * and never moves while it is up, however the player looks at the pet or walks about.
 *
 * <p>As the game draws everything in the world, what the bubble says is taken down with
 * the pet at hand, into its render state, and drawn later from that state alone.
 */
public final class SpeechBubbleRenderer {
    /** How far above the name-tag spot the bubble sits, in blocks: one line of a name tag. */
    private static final float ABOVE_NAME = 0.28F;
    /** Text pixels to blocks, as the game draws a name tag. */
    private static final float SCALE = 0.025F;
    /** How long a line takes to fade at the end of its time, in ticks. */
    private static final float FADE_TICKS = 10.0F;

    /** Takes down what the pet is saying this frame, if anything, into its render state. */
    public void extract(AbstractPet pet, ChiikawaRenderState state, float partialTick) {
        if (Minecraft.getInstance().gui.hud.isHidden()) {
            return;
        }
        pet.getSpeech().ifPresent(speech -> {
            Vec3 nameTag = pet.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, pet.getYRot(partialTick));
            if (nameTag == null) {
                return;
            }
            float left = speech.left(pet.tickCount, partialTick);
            state.put(PetData.SPEECH, new Speech(speech.line(), nameTag, Mth.clamp(left / FADE_TICKS, 0.0F, 1.0F)));
        });
    }

    public void submit(ChiikawaRenderState state, PoseStack poseStack, SubmitNodeCollector collector, Font font,
                       CameraRenderState camera) {
        Speech speech = state.get(PetData.SPEECH);
        if (speech == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(speech.nameTag().x, speech.nameTag().y + 0.5 + ABOVE_NAME, speech.nameTag().z);
        poseStack.mulPose(camera.orientation);
        poseStack.scale(SCALE, -SCALE, SCALE);
        WorldSurface surface = new WorldSurface(poseStack, collector, font, speech.alpha());
        Bubble bubble = new Bubble(Component.translatable(speech.line()).getString());
        bubble.draw(surface, -bubble.width(surface) / 2, -bubble.height(surface) - Bubble.TAIL, 0);
        poseStack.popPose();
    }

    /**
     * What a pet is saying in one frame.
     *
     * @param line the translation key of what it says
     * @param nameTag the spot where the game draws its name tag
     * @param alpha how much of the bubble shows, as it fades at the end
     */
    public record Speech(String line, Vec3 nameTag, float alpha) {
    }
}
