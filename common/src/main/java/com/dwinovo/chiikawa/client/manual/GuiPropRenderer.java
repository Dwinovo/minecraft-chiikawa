package com.dwinovo.chiikawa.client.manual;

import com.dwinovo.chiikawa.anim.render.PropRenderer;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * A prop pasted into a screen, the way the game pastes in a pet: drawn in 3D off to the
 * side, into a picture the size of the box it goes in, which then goes on the screen. The
 * handbook's panels stage their props with it.
 */
public final class GuiPropRenderer extends PictureInPictureRenderer<GuiPropRenderer.State> {
    private static final float PIXEL = 1.0F / 16.0F;

    /**
     * One prop in its box.
     *
     * @param prop the prop's id, which names its model
     * @param translation from the middle of the box to where the prop stands, in blocks
     * @param rotation how the prop is turned, after it is put there
     * @param scale screen pixels to a block
     */
    public record State(Identifier prop, Vector3f translation, Quaternionf rotation, int x0, int y0, int x1, int y1,
            float scale, @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds)
            implements PictureInPictureRenderState {
        public State(Identifier prop, Vector3f translation, Quaternionf rotation, int x0, int y0, int x1, int y1,
                float scale) {
            this(prop, translation, rotation, x0, y0, x1, y1, scale, null,
                PictureInPictureRenderState.getBounds(x0, y0, x1, y1, null));
        }
    }

    @Override
    public Class<State> getRenderStateClass() {
        return State.class;
    }

    @Override
    protected void renderToTexture(State state, PoseStack pose, SubmitNodeCollector collector) {
        Minecraft.getInstance().gameRenderer.lighting().setupFor(Lighting.Entry.ENTITY_IN_UI);
        pose.translate(state.translation().x, state.translation().y, state.translation().z);
        pose.mulPose(state.rotation());
        // A prop's model is in model pixels.
        pose.scale(PIXEL, PIXEL, PIXEL);
        PropRenderer.draw(state.prop(), pose, collector, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
    }

    @Override
    protected float getTranslateY(int height, int guiScale) {
        return height / 2.0F;
    }

    @Override
    protected String getTextureLabel() {
        return "chiikawa prop";
    }
}
