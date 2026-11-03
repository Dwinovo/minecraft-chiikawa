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
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

/**
 * What a pet is saying, in a bubble over its head, for everyone near enough to have heard
 * it — whoever the pet belongs to. F1 hides it with the rest of the HUD, a player who has
 * turned speech bubbles off in the settings does not get it at all, and it fades at the end.
 * The pet opens its mouth either way: that is the pet's, not the bubble's.
 *
 * <p>It sits where {@link OverHead} puts what is over a pet's head.
 *
 * <p>What it draws is taken from the pet with the rest of its render state, as the game
 * takes a name tag's text and spot, and drawn from that alone.
 */
public final class SpeechBubbleRenderer {
    /** How long a line takes to fade at the end of its time, in ticks. */
    private static final float FADE_TICKS = 10.0F;

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
            Vec3 nameTag = OverHead.spot(pet, partialTick);
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
        OverHead.draw(saying.nameTag(), saying.nameShown(), poseStack, bufferSource, font, cameraOrientation, saying.alpha(), surface -> {
            Bubble bubble = new Bubble(saying.words());
            bubble.draw(surface, -bubble.width(surface) / 2, -bubble.height(surface) - Bubble.TAIL, 0);
        });
    }

    /**
     * A line over a pet's head, as it is drawn this frame.
     *
     * @param words   what it says, in the player's language
     * @param nameTag where the game draws the pet's name tag, relative to the pet
     * @param nameShown whether the game draws the name tag there
     * @param alpha   how much of the bubble shows, from 0 as it fades out to 1
     */
    public record Saying(String words, Vec3 nameTag, boolean nameShown, float alpha) {
    }
}
