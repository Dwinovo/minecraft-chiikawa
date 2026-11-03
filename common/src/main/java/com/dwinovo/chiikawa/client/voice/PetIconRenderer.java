package com.dwinovo.chiikawa.client.voice;

import com.dwinovo.chiikawa.client.anim.render.ChiikawaRenderState;
import com.dwinovo.chiikawa.client.anim.render.PetData;
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
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;

/**
 * What is on a pet's mind, as a small picture on a card over its head, for everyone who looks at it: a present for its
 * owner, no work to do, short of something, frightened, asleep. F1 hides it with the rest of the HUD, and it gives way to a line
 * the pet is saying, which is drawn at the same spot.
 */
public final class PetIconRenderer {
    public void extract(AbstractPet pet, ChiikawaRenderState state, float partialTick) {
        PixelArt sprite = sprite(pet.getIcon());
        if (sprite == null || Minecraft.getInstance().options.hideGui || pet.getSpeech().isPresent()) {
            return;
        }
        Vec3 nameTag = OverHead.spot(pet, partialTick);
        if (nameTag != null) {
            state.put(PetData.ICON, new Mind(sprite, nameTag, state.nameTag != null));
        }
    }

    public void submit(ChiikawaRenderState state, PoseStack poseStack, SubmitNodeCollector collector, Font font,
                       CameraRenderState camera) {
        Mind mind = state.get(PetData.ICON);
        if (mind == null) {
            return;
        }
        PixelArt sprite = mind.sprite();
        OverHead.draw(mind.nameTag(), mind.nameShown(), poseStack, collector, font, camera, 1.0F, surface -> {
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
     * @param nameShown whether the game draws the name tag there
     */
    public record Mind(PixelArt sprite, Vec3 nameTag, boolean nameShown) {
    }

    /** @return the picture for an icon, none for no icon */
    private static PixelArt sprite(PetIcon icon) {
        return switch (icon) {
            case NONE -> null;
            case GIFT -> PixelArt.GIFT;
            case SEEKING_WORK -> PixelArt.SLIP;
            case SCARED -> PixelArt.ALERT;
            case MISSING -> PixelArt.QUESTION;
            case SLEEPY -> PixelArt.SLEEPY;
            case PRAISE -> PixelArt.PRAISE;
        };
    }
}
