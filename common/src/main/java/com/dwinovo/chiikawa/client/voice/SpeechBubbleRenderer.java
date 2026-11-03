package com.dwinovo.chiikawa.client.voice;

import com.dwinovo.chiikawa.client.anim.render.ChiikawaRenderState;
import com.dwinovo.chiikawa.client.anim.render.PetData;
import com.dwinovo.chiikawa.client.settings.ClientSettingsStore;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.ui.widget.Bubble;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * What a pet is saying, in a bubble over its head, for everyone near enough to have heard
 * it — whoever the pet belongs to. F1 hides it with the rest of the HUD, a player who has
 * turned speech bubbles off in the settings does not get it at all, and it fades at the end.
 * The pet opens its mouth either way: that is the pet's, not the bubble's.
 *
 * <p>It sits where {@link OverHead} puts what is over a pet's head.
 *
 * <p>Like a name tag it is worked out with the rest of the pet's snapshot ({@link #extract})
 * and drawn from the snapshot alone ({@link #submit}).
 */
public final class SpeechBubbleRenderer {
    /** How long a line takes to fade at the end of its time, in ticks. */
    private static final float FADE_TICKS = 10.0F;

    /** A line up over a pet this frame: its words, the pet's name-tag spot, and how much of it shows. */
    public record Shown(String line, Vec3 nameTag, float alpha) {
    }

    public void extract(AbstractPet pet, ChiikawaRenderState state, float partialTick) {
        if (Minecraft.getInstance().options.hideGui || !ClientSettingsStore.current().speechBubbles()) {
            return;
        }
        pet.getSpeech().ifPresent(speech -> {
            Vec3 nameTag = OverHead.spot(pet, partialTick);
            if (nameTag == null) {
                return;
            }
            float left = speech.left(pet.tickCount, partialTick);
            state.put(PetData.SPEECH, new Shown(Component.translatable(speech.line(),
                speech.subject().map(Component::translatable).orElse(Component.empty())).getString(), nameTag,
                Mth.clamp(left / FADE_TICKS, 0.0F, 1.0F)));
        });
    }

    public void submit(ChiikawaRenderState state, PoseStack poseStack, SubmitNodeCollector collector, Font font,
                       CameraRenderState camera) {
        Shown shown = state.get(PetData.SPEECH);
        if (shown == null) {
            return;
        }
        OverHead.draw(shown.nameTag(), poseStack, collector, font, camera, shown.alpha(), surface -> {
            Bubble bubble = new Bubble(shown.line());
            bubble.draw(surface, -bubble.width(surface) / 2, -bubble.height(surface) - Bubble.TAIL, 0);
        });
    }
}
