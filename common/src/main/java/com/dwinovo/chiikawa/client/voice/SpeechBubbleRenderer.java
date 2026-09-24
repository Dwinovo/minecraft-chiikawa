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
import net.minecraft.client.renderer.state.CameraRenderState;
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
 * <p>Like a name tag it is worked out with the rest of the pet's snapshot ({@link #extract})
 * and drawn from the snapshot alone ({@link #submit}).
 */
public final class SpeechBubbleRenderer {
    /** How far above the name-tag spot the bubble sits, in blocks: one line of a name tag. */
    private static final float ABOVE_NAME = 0.28F;
    /** Text pixels to blocks, as the game draws a name tag. */
    private static final float SCALE = 0.025F;
    /** How long a line takes to fade at the end of its time, in ticks. */
    private static final float FADE_TICKS = 10.0F;

    /** A line up over a pet this frame: its words, the pet's name-tag spot, and how much of it shows. */
    public record Shown(String line, Vec3 nameTag, float alpha) {
    }

    public void extract(AbstractPet pet, ChiikawaRenderState state, float partialTick) {
        if (Minecraft.getInstance().options.hideGui) {
            return;
        }
        pet.getSpeech().ifPresent(speech -> {
            Vec3 nameTag = pet.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, pet.getYRot(partialTick));
            if (nameTag == null) {
                return;
            }
            float left = speech.left(pet.tickCount, partialTick);
            state.put(PetData.SPEECH, new Shown(Component.translatable(speech.line()).getString(), nameTag,
                Mth.clamp(left / FADE_TICKS, 0.0F, 1.0F)));
        });
    }

    public void submit(ChiikawaRenderState state, PoseStack poseStack, SubmitNodeCollector collector, Font font,
                       CameraRenderState camera) {
        Shown shown = state.get(PetData.SPEECH);
        if (shown == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(shown.nameTag().x, shown.nameTag().y + 0.5 + ABOVE_NAME, shown.nameTag().z);
        poseStack.mulPose(camera.orientation);
        poseStack.scale(SCALE, -SCALE, SCALE);
        WorldSurface surface = new WorldSurface(poseStack, collector, font, shown.alpha());
        Bubble bubble = new Bubble(shown.line());
        bubble.draw(surface, -bubble.width(surface) / 2, -bubble.height(surface) - Bubble.TAIL, 0);
        poseStack.popPose();
    }
}
