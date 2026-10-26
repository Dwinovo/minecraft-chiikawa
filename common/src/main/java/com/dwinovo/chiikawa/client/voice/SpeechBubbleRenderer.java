package com.dwinovo.chiikawa.client.voice;

import com.dwinovo.chiikawa.client.settings.ClientSettingsStore;
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
 * <p>It sits where {@link OverHead} puts what is over a pet's head.
 */
public final class SpeechBubbleRenderer {
    /** How long a line takes to fade at the end of its time, in ticks. */
    private static final float FADE_TICKS = 10.0F;

    public void render(AbstractPet pet, PoseStack poseStack, MultiBufferSource bufferSource, Font font,
                       Quaternionf cameraOrientation, float partialTick) {
        if (Minecraft.getInstance().options.hideGui || !ClientSettingsStore.current().speechBubbles()) {
            return;
        }
        pet.getSpeech().ifPresent(speech -> {
            float left = speech.left(pet.tickCount, partialTick);
            OverHead.draw(pet, poseStack, bufferSource, font, cameraOrientation, partialTick,
                Mth.clamp(left / FADE_TICKS, 0.0F, 1.0F), surface -> {
                    Bubble bubble = new Bubble(Component.translatable(speech.line(),
                        speech.subject().map(Component::translatable).orElse(Component.empty())).getString());
                    bubble.draw(surface, -bubble.width(surface) / 2, -bubble.height(surface) - Bubble.TAIL, 0);
                });
        });
    }
}
