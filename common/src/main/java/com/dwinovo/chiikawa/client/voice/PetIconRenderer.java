package com.dwinovo.chiikawa.client.voice;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.run.PetIcon;
import com.dwinovo.chiikawa.ui.PixelArt;
import com.dwinovo.chiikawa.ui.Ui;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.UiTheme;
import com.dwinovo.chiikawa.ui.widget.Bubble;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

/**
 * What is on a pet's mind, as a small picture on a card over its head, for everyone who looks at it: a present for its
 * owner, no work to do, short of something, frightened. F1 hides it with the rest of the HUD, and it gives way to a line
 * the pet is saying, which is drawn at the same spot.
 */
public final class PetIconRenderer {
    /**
     * @return what is on the pet's mind this frame, or {@code null} when there is no card to draw
     */
    @Nullable
    public Mind extract(AbstractPet pet, float partialTick) {
        PixelArt sprite = sprite(pet.getIcon());
        if (sprite == null || Minecraft.getInstance().options.hideGui || pet.getSpeech().isPresent()) {
            return null;
        }
        Vec3 nameTag = OverHead.spot(pet, partialTick);
        return nameTag == null ? null : new Mind(sprite, nameTag);
    }

    public void render(Mind mind, PoseStack poseStack, MultiBufferSource bufferSource, Font font,
                       Quaternionf cameraOrientation) {
        PixelArt sprite = mind.sprite();
        OverHead.draw(mind.nameTag(), poseStack, bufferSource, font, cameraOrientation, 1.0F, surface -> {
            int width = sprite.width() + 2 * UiStyle.GAP;
            int height = sprite.height() + 2 * UiStyle.TIGHT;
            int x = -width / 2;
            int y = -height - Bubble.TAIL;
            Ui.sticker(surface, x, y, width, height, Ui.CARD_RADIUS, UiTheme.PANEL);
            sprite.draw(surface, x + UiStyle.GAP, y + UiStyle.TIGHT);
        });
    }

    /**
     * What is on a pet's mind, as it is drawn this frame.
     *
     * @param sprite  the picture on the card
     * @param nameTag where the game draws the pet's name tag, relative to the pet
     */
    public record Mind(PixelArt sprite, Vec3 nameTag) {
    }

    /** @return the picture for an icon, none for no icon */
    private static PixelArt sprite(PetIcon icon) {
        return switch (icon) {
            case NONE -> null;
            case GIFT -> PixelArt.GIFT;
            case SEEKING_WORK -> PixelArt.SLIP;
            case SCARED -> PixelArt.ALERT;
            case MISSING -> PixelArt.QUESTION;
        };
    }
}
