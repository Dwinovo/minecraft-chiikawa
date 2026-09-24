package com.dwinovo.chiikawa.client.render;

import com.dwinovo.chiikawa.anim.render.PropRenderer;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * Draws a prop as a picture in a screen, the way the game draws an entity in one: into a
 * picture of its own the size of the area it stands in, then onto the screen. Each loader
 * registers it beside the game's own.
 */
public final class PropPictureRenderer extends PictureInPictureRenderer<PropPictureRenderer.State> {
    private static final float PIXEL = 1.0F / 16.0F;

    public PropPictureRenderer(MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
    }

    @Override
    public Class<State> getRenderStateClass() {
        return State.class;
    }

    @Override
    protected void renderToTexture(State state, PoseStack pose) {
        Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ENTITY_IN_UI);
        pose.translate(state.translation().x, state.translation().y, state.translation().z);
        pose.mulPose(Axis.ZP.rotationDegrees(180.0F));
        // Turned the way a pet is, so a prop and a pet given the same facing face alike.
        pose.mulPose(Axis.YP.rotationDegrees(state.facing()));
        pose.scale(PIXEL, PIXEL, PIXEL);
        FeatureRenderDispatcher features = Minecraft.getInstance().gameRenderer.getFeatureRenderDispatcher();
        PropRenderer.draw(state.prop(), pose, features.getSubmitNodeStorage(), LightTexture.FULL_BRIGHT,
            OverlayTexture.NO_OVERLAY);
        features.renderAllFeatures();
    }

    @Override
    protected float getTranslateY(int height, int guiScale) {
        return height / 2.0F;
    }

    @Override
    protected String getTextureLabel() {
        return "chiikawa prop";
    }

    /**
     * One prop in a screen.
     *
     * @param translation from the middle of the area to where the prop stands, in blocks
     *                    with y running down
     * @param scale screen pixels a block
     */
    public record State(
        ResourceLocation prop,
        float facing,
        Vector3f translation,
        int x0,
        int y0,
        int x1,
        int y1,
        float scale,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds
    ) implements PictureInPictureRenderState {
        public State(ResourceLocation prop, float facing, Vector3f translation, int x0, int y0, int x1, int y1,
                float scale, @Nullable ScreenRectangle scissorArea) {
            this(prop, facing, translation, x0, y0, x1, y1, scale, scissorArea,
                PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea));
        }
    }
}
