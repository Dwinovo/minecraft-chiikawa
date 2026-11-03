package com.dwinovo.chiikawa.client.voice;

import com.dwinovo.chiikawa.client.anim.render.ChiikawaRenderState;
import com.dwinovo.chiikawa.client.anim.render.PetData;
import com.dwinovo.chiikawa.client.settings.ClientSettingsStore;
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
 * it — whoever the pet belongs to. F1 hides it with the rest of the HUD, a player who has
 * turned speech bubbles off in the settings does not get it at all, and it fades at the end.
 * The pet opens its mouth either way: that is the pet's, not the bubble's.
 *
 * <p>It is drawn on a surface turned to the camera and scaled to text pixels, as the game draws a name tag, and it sits on
 * the pet's head: just above the top of it, where the game puts the {@link EntityAttachment#NAME_TAG} attachment point,
 * which is the top of the pet's hitbox. While the game draws a name tag, which it does from half a block above that point
 * and one line of text down, it sits just above the name instead, so it moves up by a name tag as the name appears and down
 * as it goes. Whether a name is drawn is the render state's to say, {@code EntityRenderState#nameTag}; this never asks the
 * pet. Apart from that it never moves while it is up, however the player looks at the pet or walks about.
 *
 * <p>Like a name tag it is worked out with the rest of the pet's snapshot ({@link #extract})
 * and drawn from the snapshot alone ({@link #submit}).
 */
public final class SpeechBubbleRenderer {
    /** How long a line takes to fade at the end of its time, in ticks. */
    private static final float FADE_TICKS = 10.0F;
    /** Text pixels to blocks, as the game draws a name tag. */
    private static final float SCALE = 0.025F;
    /** How far above the top of the pet's head it sits when no name tag is drawn, in blocks. */
    private static final float ABOVE_HEAD = 0.1F;
    /** How far above the name-tag attachment point the game starts drawing a name tag from, in blocks: the top of the name. */
    private static final float NAME_TAG_RISE = 0.5F;
    /** How far above the name it sits when one is drawn, in blocks: four text pixels. */
    private static final float ABOVE_NAME = 4 * SCALE;

    /** A line up over a pet this frame: its words, the pet's name-tag spot and whether a name tag is drawn there, and how much of it shows. */
    public record Shown(String line, Vec3 nameTag, boolean nameShown, float alpha) {
    }

    public void extract(AbstractPet pet, ChiikawaRenderState state, float partialTick) {
        if (Minecraft.getInstance().gui.hud.isHidden() || !ClientSettingsStore.current().speechBubbles()) {
            return;
        }
        pet.getSpeech().ifPresent(speech -> {
            Vec3 nameTag = pet.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, pet.getYRot(partialTick));
            if (nameTag == null) {
                return;
            }
            float left = speech.left(pet.tickCount, partialTick);
            state.put(PetData.SPEECH, new Shown(Component.translatable(speech.line(),
                speech.subject().map(Component::translatable).orElse(Component.empty())).getString(), nameTag,
                state.nameTag != null, Mth.clamp(left / FADE_TICKS, 0.0F, 1.0F)));
        });
    }

    public void submit(ChiikawaRenderState state, PoseStack poseStack, SubmitNodeCollector collector, Font font,
                       CameraRenderState camera) {
        Shown shown = state.get(PetData.SPEECH);
        if (shown == null) {
            return;
        }
        Vec3 nameTag = shown.nameTag();
        Bubble bubble = new Bubble(shown.line());
        poseStack.pushPose();
        poseStack.translate(nameTag.x, nameTag.y + (shown.nameShown() ? NAME_TAG_RISE + ABOVE_NAME : ABOVE_HEAD), nameTag.z);
        poseStack.rotate(camera.orientation);
        poseStack.scale(SCALE, -SCALE, SCALE);
        WorldSurface surface = new WorldSurface(poseStack, collector, font, shown.alpha());
        bubble.draw(surface, -bubble.width(surface) / 2, -bubble.height(surface) - Bubble.TAIL, 0);
        poseStack.popPose();
    }
}
