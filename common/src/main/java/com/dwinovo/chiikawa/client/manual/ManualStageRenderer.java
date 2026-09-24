package com.dwinovo.chiikawa.client.manual;

import com.dwinovo.chiikawa.ui.Rect;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * Draws a page of the handbook's pets and props into the picture the game puts on the
 * screen, as the game's own {@code GuiEntityRenderer} draws the player in the inventory:
 * lit the way an entity in a screen is, with no shadow. Each panel is clipped to its frame.
 * Registered by each loader for {@link ManualStageRenderState}.
 */
public final class ManualStageRenderer extends PictureInPictureRenderer<ManualStageRenderState> {
    public ManualStageRenderer(MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
    }

    @Override
    public Class<ManualStageRenderState> getRenderStateClass() {
        return ManualStageRenderState.class;
    }

    @Override
    protected void renderToTexture(ManualStageRenderState state, PoseStack pose) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.gameRenderer.getLighting().setupFor(Lighting.Entry.ENTITY_IN_UI);
        int guiScale = minecraft.getWindow().getGuiScale();
        int textureHeight = (state.y1() - state.y0()) * guiScale;
        // From the middle of the picture's top edge, where the game starts it, to the
        // screen's top left corner, where the panels are laid out.
        pose.translate(-(state.x1() - state.x0()) / 2.0F - state.x0(), -state.y0(), 0.0F);
        minecraft.getEntityRenderDispatcher().setRenderShadow(false);
        for (ManualStageRenderState.Panel panel : state.panels()) {
            Rect clip = panel.clip();
            RenderSystem.enableScissorForRenderTypeDraws((clip.x() - state.x0()) * guiScale,
                textureHeight - (clip.bottom() - state.y0()) * guiScale, clip.width() * guiScale, clip.height() * guiScale);
            for (ManualStageRenderState.Actor actor : panel.actors()) {
                actor.draw(pose, this.bufferSource);
            }
            this.bufferSource.endBatch();
            RenderSystem.disableScissorForRenderTypeDraws();
        }
        minecraft.getEntityRenderDispatcher().setRenderShadow(true);
    }

    /** The picture's top edge, rather than its middle: the panels hang from the top. */
    @Override
    protected float getTranslateY(int height, int guiScale) {
        return 0.0F;
    }

    @Override
    protected String getTextureLabel() {
        return "chiikawa handbook";
    }
}
