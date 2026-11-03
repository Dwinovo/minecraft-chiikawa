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
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

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
 * as it goes. Whether a name is drawn is the render state's to say, {@code EntityRenderState#nameTag}, and is handed in with
 * the pet; this never asks the pet. Apart from that it never moves while it is up, however the player looks at the pet or
 * walks about.
 *
 * <p>What it draws, and where, is taken from the pet with the rest of its render state, as the game takes a name tag's text
 * and spot, and drawn from that alone.
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

    /**
     * @param nameShown whether the game draws the pet's name tag, which the bubble hangs over
     * @return what the pet is saying this frame, or {@code null} when there is no bubble to draw
     */
    @Nullable
    public Saying extract(AbstractPet pet, boolean nameShown, float partialTick) {
        if (Minecraft.getInstance().options.hideGui || !ClientSettingsStore.current().speechBubbles()) {
            return null;
        }
        return pet.getSpeech().map(speech -> {
            Vec3 nameTag = pet.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, pet.getViewYRot(partialTick));
            if (nameTag == null) {
                return null;
            }
            float left = speech.left(pet.tickCount, partialTick);
            return new Saying(Component.translatable(speech.line(),
                speech.subject().map(Component::translatable).orElse(Component.empty())).getString(), nameTag, nameShown,
                Mth.clamp(left / FADE_TICKS, 0.0F, 1.0F));
        }).orElse(null);
    }

    public void render(Saying saying, PoseStack poseStack, MultiBufferSource bufferSource, Font font,
                       Quaternionf cameraOrientation) {
        Vec3 nameTag = saying.nameTag();
        Bubble bubble = new Bubble(saying.words());
        poseStack.pushPose();
        poseStack.translate(nameTag.x, nameTag.y + (saying.nameShown() ? NAME_TAG_RISE + ABOVE_NAME : ABOVE_HEAD), nameTag.z);
        poseStack.mulPose(cameraOrientation);
        poseStack.scale(SCALE, -SCALE, SCALE);
        WorldSurface surface = new WorldSurface(poseStack, bufferSource, font, saying.alpha());
        bubble.draw(surface, -bubble.width(surface) / 2, -bubble.height(surface) - Bubble.TAIL, 0);
        poseStack.popPose();
    }

    /**
     * A line over a pet's head, as it is drawn this frame.
     *
     * @param words     what it says, in the player's language
     * @param nameTag   where the game draws the pet's name tag, relative to the pet
     * @param nameShown whether the game draws the name tag there
     * @param alpha     how much of the bubble shows, from 0 as it fades out to 1
     */
    public record Saying(String words, Vec3 nameTag, boolean nameShown, float alpha) {
    }
}
